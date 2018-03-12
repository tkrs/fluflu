package fluflu.queue

import java.time.Duration
import java.util
import java.util.concurrent._

import com.typesafe.scalalogging.LazyLogging
import fluflu.Messenger
import fluflu.msgpack.Packer

import scala.collection.mutable
import scala.collection.mutable.ListBuffer

final class ForwardConsumer private[queue] (
    val delay: Duration,
    val maximumPulls: Int,
    val messenger: Messenger,
    val scheduler: ScheduledExecutorService,
    val queue: util.Queue[() => (String, Either[Throwable, Array[Byte]])])(
    implicit PS: Packer[String])
    extends Consumer
    with LazyLogging {

  type E = () => (String, Either[Throwable, Array[Byte]])

  def consume(): Unit = {
    logger.trace(s"Start emitting. remaining: $queue")
    val start = System.nanoTime()
    val m     = mutable.Map.empty[String, ListBuffer[Array[Byte]]]
    Iterator
      .continually(queue.poll())
      .takeWhile { v =>
        logger.trace(s"Polled value: $v"); v != null
      }
      .take(maximumPulls)
      .foreach {
        _() match {
          case (s, Right(x)) =>
            if (m.contains(s)) m(s) += x
            else m += s -> ListBuffer(x)
          case (s, x @ Left(e)) =>
            logger.warn(
              s"An exception occurred during serializing record: tag: $s, cause: ${e.getMessage}",
              e)
            x
        }
      }

    val xs = m.iterator
      .map {
        case (s, vs) =>
          val acc = Array.newBuilder[Byte]
          PS.apply(s) match {
            case l @ Left(e) =>
              logger.warn(s"An exception occurred during packing tag: $s, cause: ${e.getMessage}",
                          e)
              l
            case Right(v) =>
              acc += 0x92.toByte
              acc ++= v
              Packer.formatArrayHeader(vs.size, acc)
              vs.foreach(acc ++= _)
              Right(acc.result())
          }
      }
      .collect {
        case Right(v) =>
          logger.trace(s"${v.map("%02x".format(_)).mkString(" ")}"); v
      }

    messenger.emit(xs)
    logger.trace(
      s"It spent ${TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - start)} ms in emitting messages.")
  }
}
