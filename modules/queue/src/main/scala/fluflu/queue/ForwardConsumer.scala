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

  def mkMap: mutable.Map[String, (ListBuffer[Array[Byte]], Int)] = {
    Iterator
      .continually(queue.poll())
      .takeWhile { v =>
        logger.trace(s"Polled value: $v"); v != null
      }
      .take(maximumPulls)
      .foldLeft(mutable.Map.empty[String, (ListBuffer[Array[Byte]], Int)]) {
        case (acc, f) =>
          f() match {
            case (s, Right(x)) =>
              if (!acc.contains(s)) acc += s -> (ListBuffer(x) -> x.length)
              else {
                val (l, r) = acc(s)
                l += x
                val sz = r + x.length
                acc += s -> (l -> sz)
              }
            case (s, x @ Left(e)) =>
              logger.warn(
                s"An exception occurred during serializing record: tag: $s, cause: ${e.getMessage}",
                e)
          }
          acc
      }
  }

  def mkBuffers(m: mutable.Map[String, (ListBuffer[Array[Byte]], Int)]): Iterator[Array[Byte]] = {
    m.iterator
      .map {
        case (s, (vs, sz)) =>
          PS.apply(s) match {
            case l @ Left(e) =>
              logger.warn(s"An exception occurred during packing tag: $s, cause: ${e.getMessage}",
                          e)
              l
            case Right(v) =>
              val arr  = Packer.formatArrayHeader(vs.size)
              val dest = Array.ofDim[Byte](sz + 1 + v.length + arr.length)
              dest(0) = 0x92.toByte
              java.lang.System.arraycopy(v, 0, dest, 1, v.length)
              java.lang.System.arraycopy(arr, 0, dest, 1 + v.length, arr.length)
              val xs = vs.scanLeft(arr.length + v.length + 1)((acc, a) => acc + a.length)

              vs.zip(xs).foreach {
                case (l, r) =>
                  java.lang.System.arraycopy(l, 0, dest, r, l.length)
              }

              Right(dest)
          }
      }
      .collect {
        case Right(v) =>
          logger.trace(s"${v.map("%02x".format(_)).mkString(" ")}"); v
      }
  }

  def consume(): Unit = {
    logger.trace(s"Start emitting. remaining: $queue")
    val start  = System.nanoTime()
    val buffer = mkBuffers(mkMap)
    messenger.emit(buffer)
    logger.trace(
      s"It spent ${TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - start)} ms in emitting messages.")
  }
}
