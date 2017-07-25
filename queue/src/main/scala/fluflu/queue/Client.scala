package fluflu
package queue

import java.time.Duration
import java.util.concurrent._
import java.util.concurrent.atomic.AtomicBoolean

import cats.syntax.either._
import com.typesafe.scalalogging.LazyLogging
import io.circe.Encoder
import monix.eval.{ Callback, Task }
import monix.execution.Scheduler

trait Client {
  def emit[A: Encoder](e: Event[A]): Either[Exception, Unit]
  def remaining: Int
  def close(): Unit
}

object Client {

  val ChunkSize = 1000

  def apply(
    delay: Duration = Duration.ofSeconds(1),
    terminationDelay: Duration = Duration.ofSeconds(10))(implicit messenger: Messenger, consumeScheduler: Scheduler): Client =
    new ClientImpl(delay, terminationDelay)

  final class ClientImpl(
    delay: Duration,
    terminationDelay: Duration)(implicit messenger: Messenger, taskScheduler: Scheduler) extends Client with LazyLogging {

    type Elm = () => Either[Throwable, Letter]

    private[this] val scheduler = Executors.newSingleThreadScheduledExecutor()

    private[this] val msgQueue: ConcurrentLinkedQueue[Elm] =
      new ConcurrentLinkedQueue()

    def emit[A: Encoder](e: Event[A]): Either[Exception, Unit] =
      if (!scheduler.isShutdown)
        Producer.emit(e).map(_ => Consumer.start())
      else
        new Exception("A Client scheduler was already shutdown").asLeft

    def remaining: Int = msgQueue.size

    def close(): Unit = {
      if (!msgQueue.isEmpty) logger.debug(s"A message queue has remaining: ${msgQueue.size()}")
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

      private[this] val write: Elm => Task[Unit] = fn =>
        fn().fold(
          e => Task.pure(logger.warn(s"Message decoding failed: ${e.getMessage}. Thus, it skips writing.")), // TODO: enhance message.
          l => messenger.write(l))

      private def consume(): Unit = {
        logger.trace(s"Start emitting. remaining: $remaining")
        val start = System.nanoTime()
        val tasks =
          Iterator.continually(msgQueue.poll())
            .takeWhile { v => logger.trace(s"Polled value: $v"); v != null }
            .map(write)
            .take(ChunkSize)
        Task.gatherUnordered(tasks).runAsync(new Callback[List[Unit]] {
          override def onError(ex: Throwable): Unit =
            logger.error(s"An exception occurred during consuming messages. cause: ${ex.getMessage}", ex)
          override def onSuccess(value: List[Unit]): Unit = ()
        })
        logger.trace(s"It spent ${TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - start)} ms in emitting messages.")
      }

      override def run(): Unit =
        if (msgQueue.isEmpty) running.set(false)
        else {
          consume()
          running.set(false)
          if (!(scheduler.isShutdown || msgQueue.isEmpty)) start()
        }

      def close(): Unit = {
        awaitTermination(scheduler, terminationDelay)
        consume()
        messenger.close()
      }
    }
  }
}
