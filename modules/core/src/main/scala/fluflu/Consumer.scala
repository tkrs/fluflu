package fluflu

import java.nio.ByteBuffer
import java.nio.charset.StandardCharsets._
import java.util
import java.util.Base64
import java.util.UUID
import java.util.concurrent.ConcurrentLinkedQueue

import com.typesafe.scalalogging.LazyLogging
import fluflu.msgpack.Ack
import fluflu.msgpack.MOption
import fluflu.msgpack.Packer
import fluflu.msgpack.Unpacker
import org.msgpack.core.MessageBufferPacker
import org.msgpack.core.MessagePack
import org.msgpack.core.MessagePack.PackerConfig

import scala.collection.mutable
import scala.collection.mutable.ListBuffer
import scala.util.Failure
import scala.util.Success
import scala.util.control.NonFatal

trait Consumer {
  type E

  protected def msgQueue: util.Queue[E]

  def consume(): Unit
}

final class ForwardConsumer private[fluflu] (
  maximumPulls: Int,
  connection: Connection,
  val msgQueue: util.Queue[(String, MessageBufferPacker => Unit)],
  packerConfig: PackerConfig = MessagePack.DEFAULT_PACKER_CONFIG
)(implicit
  PS: Packer[String],
  PM: Packer[MOption],
  UA: Unpacker[Option[Ack]]
) extends Consumer
    with LazyLogging {
  private[this] val errorQueue: util.Queue[(String, ByteBuffer)] = new ConcurrentLinkedQueue[(String, ByteBuffer)]

  private[this] val mPacker = new ThreadLocal[MessageBufferPacker] {
    override def initialValue(): MessageBufferPacker = packerConfig.newBufferPacker()
  }

  private[this] val b64e = Base64.getEncoder

  type E = (String, MessageBufferPacker => Unit)

  def retrieveElements(): Map[String, ListBuffer[MessageBufferPacker => Unit]] =
    Iterator
      .continually(msgQueue.poll())
      .take(maximumPulls)
      .takeWhile(_ != null)
      .foldLeft(mutable.Map.empty[String, ListBuffer[MessageBufferPacker => Unit]]) { case (acc, (k, f)) =>
        acc += k -> (acc.getOrElse(k, ListBuffer.empty) += f)
      }
      .toMap

  def makeMessage(s: String, fs: ListBuffer[MessageBufferPacker => Unit]): Option[(String, ByteBuffer)] = {
    val packer = mPacker.get()
    try {
      val chunk = b64e.encodeToString(UUID.randomUUID().toString.getBytes(UTF_8))
      logger.trace(s"tag: $s, chunk: $chunk")
      packer.packArrayHeader(3)
      PS.apply(s, packer)
      packer.packArrayHeader(fs.size)
      fs.foreach(_(packer))
      PM.apply(MOption(chunk = Some(chunk)), packer)
      Some(chunk -> packer.toMessageBuffer.sliceAsByteBuffer())
    } catch {
      case NonFatal(e) =>
        logger.error(s"Failed to make a message: $e")
        None
    } finally packer.clear()
  }

  def makeMessages(m: Map[String, ListBuffer[MessageBufferPacker => Unit]]): Iterator[(String, ByteBuffer)] =
    m.iterator.map((makeMessage _).tupled).collect { case Some(v) => v }

  private def send(chunk: String, msg: ByteBuffer): Unit =
    connection.writeAndRead(msg) match {
      case Success(a) =>
        UA.apply(a) match {
          case Right(Some(b)) if b.ack == chunk =>
            logger.trace(s"Succeeded to write a message")
          case Right(b) =>
            logger.warn(s"Ack-ID and chunk did not match: ${b.fold("")(_.ack)} ≠ $chunk")
            msg.flip()
            errorQueue.offer(chunk -> msg)
          case Left(e) =>
            logger.warn(s"Failed to decode a response message: $e")
            msg.flip()
            errorQueue.offer(chunk -> msg)
        }

      case Failure(e) =>
        logger.warn(s"Failed to write a message: $msg, error: $e")
        msg.flip()
        errorQueue.offer(chunk -> msg)
    }

  def retrieveErrors(): Iterator[(String, ByteBuffer)] =
    Iterator.continually(errorQueue.poll()).takeWhile(_ != null).take(maximumPulls)

  def consume(): Unit =
    if (msgQueue.isEmpty && errorQueue.isEmpty) ()
    else {
      retrieveErrors().foreach((send _).tupled)
      makeMessages(retrieveElements()).foreach((send _).tupled)
    }
}
