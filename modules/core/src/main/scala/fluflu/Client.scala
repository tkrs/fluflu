package fluflu

import java.time.{Duration, Instant}
import java.util.concurrent.{ConcurrentLinkedQueue, Executors}

import fluflu.msgpack.Packer
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
      messenger: Messenger,
      PS: Packer[String],
      PI: Packer[Instant]): Client = {
    val scheduler = Executors.newSingleThreadScheduledExecutor()
    val queue     = new ConcurrentLinkedQueue[() => Array[Byte]]
    val consumer  = new DefaultConsumer(delay, maximumPulls, messenger, scheduler, queue)

    new Client {
      private[this] val mPacker = new ThreadLocal[MessageBufferPacker] {
        override def initialValue(): MessageBufferPacker = packerConfig.newBufferPacker()
      }
      def emit[A: Packer](tag: String, record: A, time: Instant): Either[Exception, Unit] =
        if (scheduler.isShutdown)
          Left(new Exception("A Client scheduler was already shutdown"))
        else {
          val fa = () =>
            try {
              val p = mPacker.get()
              Packer[(String, A, Instant)].apply((tag, record, time), p)
              p.toByteArray
            } finally mPacker.get().clear()
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
                  maximumPulls: Int = 1000,
                  packerConfig: PackerConfig = MessagePack.DEFAULT_PACKER_CONFIG)(
      implicit
      messenger: Messenger,
      PS: Packer[String],
      PI: Packer[Instant]): Client = {
    val scheduler = Executors.newSingleThreadScheduledExecutor()
    val queue     = new ConcurrentLinkedQueue[() => (String, Array[Byte])]
    val consumer  = new ForwardConsumer(delay, maximumPulls, messenger, scheduler, queue)

    new Client {
      private[this] val mPacker = new ThreadLocal[MessageBufferPacker] {
        override def initialValue(): MessageBufferPacker = packerConfig.newBufferPacker()
      }

      def emit[A: Packer](tag: String, record: A, time: Instant): Either[Exception, Unit] =
        if (scheduler.isShutdown)
          Left(new Exception("A Client scheduler was already shutdown"))
        else {
          val fa = () =>
            try {
              val p = mPacker.get
              Packer[(A, Instant)].apply((record, time), p)
              (tag, p.toByteArray)
            } finally mPacker.get.clear()
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
