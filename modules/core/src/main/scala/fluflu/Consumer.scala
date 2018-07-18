package fluflu

import java.nio.ByteBuffer
import java.nio.charset.StandardCharsets._
import java.util
import java.util.{Base64, UUID}

import fluflu.msgpack.{Ack, MOption, Packer, Unpacker}

import scala.util.control.NonFatal
import com.typesafe.scalalogging.LazyLogging
import org.msgpack.core.MessagePack.PackerConfig
import org.msgpack.core.{MessageBufferPacker, MessagePack}

import scala.annotation.tailrec
import scala.collection.mutable
import scala.collection.mutable.ListBuffer
import scala.util.{Failure, Success, Try}

trait Consumer extends Runnable {
  type E

  protected def msgQueue: util.Queue[E]
  // protected def errorQueue: util.Queue[ByteBuffer]

  def consume(): Unit

  def run(): Unit = consume()
}

final class ForwardConsumer private[fluflu] (val maximumPulls: Int,
                                             val connection: Connection,
                                             val msgQueue: util.Queue[(String, MessageBufferPacker => Unit)],
                                             val packerConfig: PackerConfig = MessagePack.DEFAULT_PACKER_CONFIG)(
    implicit PS: Packer[String],
    PM: Packer[MOption],
    PA: Unpacker[Ack]
) extends Consumer
    with LazyLogging {

  private[this] val mPacker = new ThreadLocal[MessageBufferPacker] {
    override def initialValue(): MessageBufferPacker = packerConfig.newBufferPacker()
  }

  private[this] val b64e = Base64.getEncoder

  type E = (String, MessageBufferPacker => Unit)

  def retrieveElements(): Map[String, ListBuffer[MessageBufferPacker => Unit]] = {
    Iterator
      .continually(msgQueue.poll())
      .takeWhile { _ != null }
      .take(maximumPulls)
      .foldLeft(mutable.Map.empty[String, ListBuffer[MessageBufferPacker => Unit]]) {
        case (acc, (k, f)) =>
          acc += k -> (acc.getOrElse(k, ListBuffer.empty) += f)
      }
      .toMap
  }

  def makeMessages(m: Map[String, ListBuffer[MessageBufferPacker => Unit]]): Iterator[(String, ByteBuffer)] = {
    m.iterator
      .map {
        case (s, fs) =>
          val packer = mPacker.get()
          try {
            val chunk = b64e.encodeToString(UUID.randomUUID().toString.getBytes(UTF_8))
            logger.trace(s"tag: $s, chunk: $chunk")
            packer.packArrayHeader(3)
            PS.apply(s, packer)
            packer.packArrayHeader(fs.size)
            fs.foreach(_(packer))
            PM.apply(MOption(chunk = Some(chunk)), packer)
            Right(chunk -> packer.toMessageBuffer.sliceAsByteBuffer())
          } catch {
            case NonFatal(e) =>
              logger.error(s"$e")
              Left(e)
          } finally packer.clear()
      }
      .collect {
        case Right(v) => v
      }
  }

  def consume(): Unit =
    if (msgQueue.isEmpty) ()
    else {
      val buffers = makeMessages(retrieveElements())

      buffers.foreach {
        case (chunk, msg) =>
          @tailrec def wr(count: Int): Try[ByteBuffer] = {
            connection.writeAndRead(msg) match {
              case a @ Success(_) => a
              case Failure(e) =>
                if (count == 0) Failure(e)
                else {
                  logger.warn(s"Cannot read or write: $e")
                  Thread.sleep(5)
                  msg.flip()
                  wr(count - 1)
                }
            }
          }

          wr(15) match {
            case Success(a) =>
              PA.apply(a) match {
                case Right(b) if b.ack == chunk =>
                  logger.trace(s"Succeeded to write a message")
                case Right(b) =>
                  logger.error(s"Ack ≠ Chunk: ${b.ack}, $chunk")
                case Left(e) =>
                  logger.error(s"Failed to decode response message: $e")
              }

            case Failure(e) => logger.error(s"Failed to write a message: $msg, error: $e")
          }
      }
    }
}
