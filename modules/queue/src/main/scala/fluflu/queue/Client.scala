package fluflu
package queue

import java.time.{Duration, Instant}
import java.util.concurrent.{ConcurrentLinkedQueue, Executors}

import fluflu.msgpack.Packer

trait Client {

  def emit[A: Packer](tag: String, record: A): Either[Exception, Unit] =
    emit(tag, record, Instant.now)

  def emit[A: Packer](tag: String, record: A, time: Instant): Either[Exception, Unit]

  def close(): Unit
}

object Client {

  def apply(delay: Duration = Duration.ofSeconds(1),
            terminationDelay: Duration = Duration.ofSeconds(10),
            maximumPulls: Int = 1000)(implicit
                                      messenger: Messenger,
                                      PS: Packer[String],
                                      PI: Packer[Instant]): Client = {
    val scheduler = Executors.newSingleThreadScheduledExecutor()
    val queue     = new ConcurrentLinkedQueue[() => Either[Throwable, Array[Byte]]]
    val consumer  = new DefaultConsumer(delay, maximumPulls, messenger, scheduler, queue)

    new Client {
      def emit[A: Packer](tag: String, record: A, time: Instant): Either[Exception, Unit] =
        if (scheduler.isShutdown)
          Left(new Exception("A Client scheduler was already shutdown"))
        else {
          val fa = () => Packer[(String, A, Instant)].apply((tag, record, time))
          if (queue.offer(fa)) Right(Consumer.start(consumer))
          else Left(new Exception("A queue no space is currently available"))
        }

      def close(): Unit = {
        awaitTermination(scheduler, terminationDelay)
        consumer.consume()
      }
    }
  }

  def forwardable(delay: Duration = Duration.ofSeconds(1),
                  terminationDelay: Duration = Duration.ofSeconds(10),
                  maximumPulls: Int = 1000)(implicit
                                            messenger: Messenger,
                                            PS: Packer[String],
                                            PI: Packer[Instant]): Client = {
    val scheduler = Executors.newSingleThreadScheduledExecutor()
    val queue     = new ConcurrentLinkedQueue[() => (String, Either[Throwable, Array[Byte]])]
    val consumer  = new ForwardConsumer(delay, maximumPulls, messenger, scheduler, queue)

    new Client {
      def emit[A: Packer](tag: String, record: A, time: Instant): Either[Exception, Unit] =
        if (scheduler.isShutdown)
          Left(new Exception("A Client scheduler was already shutdown"))
        else {
          val fa = () => (tag, Packer[(A, Instant)].apply((record, time)))
          if (queue.offer(fa)) Right(Consumer.start(consumer))
          else Left(new Exception("A queue no space is currently available"))
        }

      def close(): Unit = {
        awaitTermination(scheduler, terminationDelay)
        consumer.consume()
      }
    }
  }
}
