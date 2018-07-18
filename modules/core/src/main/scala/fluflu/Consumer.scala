package fluflu

import java.time.Duration
import java.util
import java.util.concurrent._
import java.util.concurrent.atomic.AtomicBoolean

import com.typesafe.scalalogging.LazyLogging

trait Consumer extends Runnable {
  type E

  protected val delay: Duration
  protected val scheduler: ScheduledExecutorService
  protected val packQueue: util.Queue[E]

  protected val running = new AtomicBoolean(false)

  def consume(): Unit

  def run(): Unit =
    if (packQueue.isEmpty) running.set(false)
    else {
      consume()
      running.set(false)
      if (!(scheduler.isShutdown || packQueue.isEmpty)) Consumer.start(this)
    }
}

object Consumer extends LazyLogging {

  def start(c: Consumer): Unit =
    if (c.running.compareAndSet(false, true)) {
      logger.trace(s"Reschedule consuming to start after [${c.delay.toNanos} nanoseconds]")
      c.scheduler.schedule(c, c.delay.toNanos, TimeUnit.NANOSECONDS)
    }
}

final class DefaultConsumer private[fluflu] (val delay: Duration,
                                             val maximumPulls: Int,
                                             val messenger: Messenger,
                                             val scheduler: ScheduledExecutorService,
                                             val packQueue: util.Queue[() => Array[Byte]])
    extends Consumer
    with LazyLogging {

  type E = () => Array[Byte]

  def consume(): Unit = {
    logger.trace(s"Start emitting. remaining: $packQueue")
    val start = System.nanoTime()
    val tasks =
      Iterator
        .continually(packQueue.poll())
        .takeWhile { v =>
          logger.trace(s"Polled value: $v"); v != null
        }
        .take(maximumPulls)
        .map(_())
    messenger.emit(tasks)
    logger.trace(
      s"It spent ${TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - start)} ms in emitting messages.")
  }
}
