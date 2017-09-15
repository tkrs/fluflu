package fluflu
package queue

import java.time.Duration
import java.util.concurrent._
import java.util.concurrent.atomic.AtomicBoolean

import cats.syntax.either._
import com.typesafe.scalalogging.LazyLogging
import io.circe.Encoder

trait Client {
  def emit[A: Encoder](e: Event[A]): Either[Exception, Unit]
  def remaining: Int
  def close(): Unit
}

object Client {

  def apply(delay: Duration = Duration.ofSeconds(1),
            terminationDelay: Duration = Duration.ofSeconds(10),
            maximumPulls: Int = 1000)(implicit messenger: Messenger): Client =
    new ClientImpl(delay, terminationDelay, maximumPulls)
  final class ClientImpl(delay: Duration, terminationDelay: Duration, maximumPulls: Int)(
      implicit messenger: Messenger)
      extends Client
      with LazyLogging {

    private[this] val scheduler = Executors.newSingleThreadScheduledExecutor()

    private[this] val msgQueue: ConcurrentLinkedQueue[Messenger#Elm] =
      new ConcurrentLinkedQueue()

    def emit[A: Encoder](e: Event[A]): Either[Exception, Unit] =
      if (!scheduler.isShutdown)
        Producer.emit(e).map(_ => Consumer.start())
      else
        new Exception("A Client scheduler was already shutdown").asLeft

    def remaining: Int = msgQueue.size

    def close(): Unit = {
      if (!msgQueue.isEmpty)
        logger.debug(s"A message queue has remaining: ${msgQueue.size()}")
      Consumer.close()
    }

    object Producer {
      def emit[A: Encoder](e: Event[A]): Either[Exception, Unit] =
        if (msgQueue offer (() => e.pack.map(Letter))) ().asRight
        else new Exception("A queue no space is currently available").asLeft
    }

    object Consumer extends Runnable {

      private[this] val running: AtomicBoolean = new AtomicBoolean(false)

      def start(): Unit =
        if (running.compareAndSet(false, true)) {
          logger.trace(s"Reschedule consuming to start after [${delay.toNanos} nanoseconds]")
          scheduler.schedule(this, delay.toNanos, TimeUnit.NANOSECONDS)
        }

      private def consume(): Unit = {
        logger.trace(s"Start emitting. remaining: $remaining")
        val start = System.nanoTime()
        val tasks =
          Iterator
            .continually(msgQueue.poll())
            .takeWhile { v =>
              logger.trace(s"Polled value: $v"); v != null
            }
            .take(maximumPulls)
        messenger.emit(tasks)
        logger.trace(
          s"It spent ${TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - start)} ms in emitting messages.")
      }

      override def run(): Unit =
        if (msgQueue.isEmpty) running.set(false)
        else {
          consume()
          running.set(false)
          if (!(scheduler.isShutdown || msgQueue.isEmpty)) start()
        }

      def close(): Unit =
        try {
          awaitTermination(scheduler, terminationDelay)
          consume()
        } finally {
          messenger.close()
        }
    }
  }
}
