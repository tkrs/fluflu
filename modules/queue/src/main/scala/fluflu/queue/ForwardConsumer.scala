package fluflu.queue

import java.time.Duration
import java.util
import java.util.concurrent._

import com.typesafe.scalalogging.LazyLogging
import fluflu.Messenger
import fluflu.msgpack.Packer
import org.msgpack.core.{MessageBufferPacker, MessagePack}
import org.msgpack.core.MessagePack.PackerConfig

import scala.collection.mutable
import scala.collection.mutable.ListBuffer

final class ForwardConsumer private[queue] (
    val delay: Duration,
    val maximumPulls: Int,
    val messenger: Messenger,
    val scheduler: ScheduledExecutorService,
    val packQueue: util.Queue[() => (String, Array[Byte])],
    val packerConfig: PackerConfig = MessagePack.DEFAULT_PACKER_CONFIG)(implicit PS: Packer[String])
    extends Consumer
    with LazyLogging {

  private[this] val mPacker = new ThreadLocal[MessageBufferPacker] {
    override def initialValue(): MessageBufferPacker = packerConfig.newBufferPacker()
  }

  type E = () => (String, Array[Byte])

  def mkMap: mutable.Map[String, ListBuffer[Array[Byte]]] = {
    Iterator
      .continually(packQueue.poll())
      .takeWhile { v =>
        logger.trace(s"Polled value: $v"); v != null
      }
      .take(maximumPulls)
      .foldLeft(mutable.Map.empty[String, ListBuffer[Array[Byte]]]) {
        case (acc, f) =>
          f() match {
            case (s, x) =>
              if (!acc.contains(s)) acc += s -> ListBuffer(x)
              else {
                val l = acc(s)
                l += x
                acc += s -> l
              }
          }
          acc
      }
  }

  def mkBuffers(m: mutable.Map[String, ListBuffer[Array[Byte]]]): Iterator[Array[Byte]] = {
    m.iterator.map {
      case (s, vs) =>
        try {
          val p = mPacker.get()
          p.packArrayHeader(2)
          PS.apply(s, p)
          p.packArrayHeader(vs.size)
          vs.foreach(p.writePayload)
          p.toByteArray
        } finally mPacker.get().clear()
    }
  }

  def consume(): Unit = {
    logger.trace(s"Start emitting. remaining: $packQueue")
    val start  = System.nanoTime()
    val buffer = mkBuffers(mkMap)
    messenger.emit(buffer)
    logger.trace(
      s"It spent ${TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - start)} ms in emitting messages.")
  }
}
