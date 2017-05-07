package fluflu
package queue

import java.time.Duration
import java.util.concurrent._
import java.util.concurrent.atomic.AtomicBoolean

import cats.syntax.either._
import com.typesafe.scalalogging.LazyLogging
import io.circe.Encoder

trait Async {
  def emit[A: Encoder](e: Event[A]): Either[Exception, Unit]
  def remaining: Int
  def close(): Unit
}

object Async {

  def apply(
    delay: Duration = Duration.ofSeconds(1),
    terminationDelay: Duration = Duration.ofSeconds(10)
  )(implicit messenger: Messenger): Async =
    new BQ(delay, terminationDelay)

  final class BQ(
      delay: Duration,
      terminationDelay: Duration
  )(implicit messenger: Messenger) extends Async with LazyLogging {

    type F = () => Either[Throwable, Letter]

    private[this] val msgQueue: ConcurrentLinkedQueue[F] =
      new ConcurrentLinkedQueue()

    private[this] val write: F => Unit = { fn =>
      fn().flatMap(messenger.write).fold(logger.error(s"Failed to send a message", _), _ => ())
    }

    def remaining: Int = msgQueue.size

    private def consume(): Unit = synchronized {
      logger.debug("Start emitting.")
      val start = System.nanoTime()
      Iterator.continually(msgQueue.poll()).takeWhile(_ != null).foreach(write)
      logger.debug(s"A emitting spend ${TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - start)} ms.")
    }

    def emit[A: Encoder](e: Event[A]): Either[Exception, Unit] =
      if (msgQueue offer (() => Messages.pack(e).map(Letter))) Either.right(R.start())
      else Either.left(new Exception("A queue no space is currently available"))

    def close(): Unit = {
      R.close()
      if (!msgQueue.isEmpty) logger.debug(s"A message queue has remaining: ${msgQueue.size()}")
      consume()
      messenger.close()
    }

    object R extends Runnable {

      private[this] val scheduler = Executors.newSingleThreadScheduledExecutor()

      private[this] val running: AtomicBoolean = new AtomicBoolean(false)

      def start(): Unit = {
        if (running.compareAndSet(false, true))
          scheduler.schedule(this, delay.toNanos, TimeUnit.NANOSECONDS)
      }

      override def run(): Unit = if (!msgQueue.isEmpty) {
        consume()
        running.set(false)
        if (!scheduler.isShutdown) start
      }

      def close(): Unit = {
        awaitTermination(scheduler, terminationDelay)
        messenger.close()
      }
    }
  }
}
