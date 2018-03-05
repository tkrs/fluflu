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
    val consumer  = new Consumer(delay, terminationDelay, maximumPulls, messenger, scheduler, queue)
    val producer  = new Producer(scheduler, queue)

    new Client {
      def emit[A: Packer](tag: String, record: A, time: Instant): Either[Exception, Unit] = {
        producer.emit(tag, record, time) match {
          case Right(_) => Right(Consumer.start(consumer))
          case l        => l
        }
      }

      def close(): Unit = {
        consumer.close()
      }
    }
  }
}
