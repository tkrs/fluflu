package fluflu
package queue

import java.time.{Duration, Instant}
import java.util.concurrent._
import java.util.concurrent.atomic.AtomicBoolean

import com.typesafe.scalalogging.LazyLogging
import fluflu.msgpack.Packer

trait Client {
  def emit[A: Packer](tag: String, record: A): Either[Exception, Unit]
  def emit[A: Packer](tag: String, record: A, time: Instant): Either[Exception, Unit]
  def remaining: Int
  def close(): Unit
}

object Client {

  def apply(delay: Duration = Duration.ofSeconds(1),
            terminationDelay: Duration = Duration.ofSeconds(10),
            maximumPulls: Int = 1000)(implicit
                                      messenger: Messenger,
                                      PS: Packer[String],
                                      PI: Packer[Instant]): Client =
    new ClientImpl(delay, terminationDelay, maximumPulls)

  final class ClientImpl(delay: Duration, terminationDelay: Duration, maximumPulls: Int)(
      implicit
      messenger: Messenger,
      PS: Packer[String],
      PI: Packer[Instant])
      extends Client
      with LazyLogging {

    private[this] val scheduler = Executors.newSingleThreadScheduledExecutor()

    private[this] val msgQueue: ConcurrentLinkedQueue[() => Either[Throwable, Array[Byte]]] =
      new ConcurrentLinkedQueue()

    def emit[A: Packer](tag: String, record: A): Either[Exception, Unit] =
      emit(tag, record, Instant.now)

    def emit[A: Packer](tag: String, record: A, time: Instant): Either[Exception, Unit] =
      if (scheduler.isShutdown)
        Left(new Exception("A Client scheduler was already shutdown"))
      else
        Producer.emit(tag, record, time) match {
          case Right(_) => Right(Consumer.start())
          case l        => l
        }

    def remaining: Int = msgQueue.size

    def close(): Unit = {
      if (!msgQueue.isEmpty)
        logger.debug(s"A message queue has remaining: ${msgQueue.size()}")
      Consumer.close()
    }

    object Producer {

      def emit[A: Packer](tag: String, record: A, time: Instant): Either[Exception, Unit] = {
        val fa = () => Packer[(String, A, Instant)].apply((tag, record, time))
        if (msgQueue.offer(fa)) Right(())
        else Left(new Exception("A queue no space is currently available"))
      }
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
            .map {
              _() match {
                case x @ Right(_) => x
                case x @ Left(e)  => logger.warn(e.getMessage); x
              }
            }
            .collect {
              case Right(v) => v
            }
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
