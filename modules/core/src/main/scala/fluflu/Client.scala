package fluflu

import java.time.{Duration, Instant}
import java.util.concurrent.{ConcurrentLinkedQueue, Executors, TimeUnit}

import com.typesafe.scalalogging.LazyLogging
import fluflu.msgpack.{Ack, MOption, Packer, Unpacker}
import org.msgpack.core.MessagePack.PackerConfig
import org.msgpack.core.{MessageBufferPacker, MessagePack}

trait Client {

  def emit[A: Packer](tag: String, record: A): Either[Exception, Unit] =
    emit(tag, record, Instant.now)

  def emit[A: Packer](tag: String, record: A, time: Instant): Either[Exception, Unit]

  def close(): Unit
}

object Client {

  def apply(delay: Duration = Duration.ofSeconds(1),
            terminationDelay: Duration = Duration.ofSeconds(10),
            maximumPulls: Int = 1000,
            packerConfig: PackerConfig = MessagePack.DEFAULT_PACKER_CONFIG)(
      implicit
      connection: Connection,
      PS: Packer[String],
      PI: Packer[Instant],
      PM: Packer[MOption],
      PA: Unpacker[Option[Ack]]
  ): Client =
    new Client with LazyLogging {
      private[this] val queue     = new ConcurrentLinkedQueue[(String, MessageBufferPacker => Unit)]
      private[this] val consumer  = new ForwardConsumer(maximumPulls, connection, queue, packerConfig)
      private[this] val scheduler = Executors.newSingleThreadScheduledExecutor()

      val _ = scheduler.scheduleWithFixedDelay(consumer, 500, delay.toNanos, TimeUnit.NANOSECONDS)

      def emit[A: Packer](tag: String, record: A, time: Instant): Either[Exception, Unit] =
        if (scheduler.isShutdown)
          Left(new Exception("Client scheduler was already shutdown"))
        else {
          logger.trace(s"Queueing message: ${(tag, record, time)}")
          val fa = (p: MessageBufferPacker) => Packer[(A, Instant)].apply((record, time), p)
          if (queue.offer(tag -> fa)) Right(())
          else Left(new Exception("The queue no space is currently available"))
        }

      def close(): Unit = {
        awaitTermination(scheduler, terminationDelay)
        consumer.consume()
      }
    }
}
