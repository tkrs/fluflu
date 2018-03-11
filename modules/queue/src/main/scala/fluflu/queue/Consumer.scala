package fluflu
package queue

import java.time.Duration
import java.util
import java.util.concurrent._
import java.util.concurrent.atomic.AtomicBoolean

import com.typesafe.scalalogging.LazyLogging

final class Consumer private[queue] (val delay: Duration,
                                     val maximumPulls: Int,
                                     val messenger: Messenger,
                                     val scheduler: ScheduledExecutorService,
                                     val queue: util.Queue[() => Either[Throwable, Array[Byte]]])
    extends Runnable
    with LazyLogging {

  private val running = new AtomicBoolean(false)

  def consume(): Unit = {
    logger.trace(s"Start emitting. remaining: $queue")
    val start = System.nanoTime()
    val tasks =
      Iterator
        .continually(queue.poll())
        .takeWhile { v =>
          logger.trace(s"Polled value: $v"); v != null
        }
        .take(maximumPulls)
        .map {
          _() match {
            case x @ Right(_) => x
            case x @ Left(e)  => logger.warn(s"An exception occurred: ${e.getMessage}", e); x
          }
        }
        .collect {
          case Right(v) => v
        }
    messenger.emit(tasks)
    logger.trace(
      s"It spent ${TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - start)} ms in emitting messages.")
  }

  def run(): Unit =
    if (queue.isEmpty) running.set(false)
    else {
      consume()
      running.set(false)
      if (!(scheduler.isShutdown || queue.isEmpty)) Consumer.start(this)
    }
}

object Consumer extends LazyLogging {

  def start(c: Consumer): Unit =
    if (c.running.compareAndSet(false, true)) {
      logger.trace(s"Reschedule consuming to start after [${c.delay.toNanos} nanoseconds]")
      c.scheduler.schedule(c, c.delay.toNanos, TimeUnit.NANOSECONDS)
    }
}
