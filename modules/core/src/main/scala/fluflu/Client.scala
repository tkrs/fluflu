package fluflu

import java.time.Instant
import java.util.concurrent._
import java.util.concurrent.atomic.AtomicBoolean

import com.typesafe.scalalogging.LazyLogging
import fluflu.msgpack.{Ack, MOption, Packer, Unpacker}
import org.msgpack.core.MessagePack.PackerConfig
import org.msgpack.core.{MessageBufferPacker, MessagePack}

import scala.concurrent.duration._

trait Client {

  final def emit[A: Packer](tag: String, record: A): Either[Exception, Unit] =
    emit(tag, record, Instant.now)

  def size: Int

  def emit[A: Packer](tag: String, record: A, time: Instant): Either[Exception, Unit]

  def close(): Unit

  def isClosed: Boolean
}

object Client {

  def apply(
    terminationTimeout: FiniteDuration = FiniteDuration(10, SECONDS),
    maximumPulls: Int = 1000,
    packerConfig: PackerConfig = MessagePack.DEFAULT_PACKER_CONFIG
  )(
    implicit
    connection: Connection,
    PS: Packer[String],
    PI: Packer[Instant],
    PM: Packer[MOption],
    PA: Unpacker[Option[Ack]]
  ): Client =
    new Client with LazyLogging {
      @volatile private[this] var closed = false

      private def scheduler(name: String) =
        Executors.newScheduledThreadPool(1, namedThreadFactory(name))

      private[this] val running  = new AtomicBoolean()
      private[this] val queue    = new ConcurrentLinkedQueue[(String, MessageBufferPacker => Unit)]
      private[this] val consumer = new ForwardConsumer(maximumPulls, connection, queue, packerConfig)
      private[this] val worker   = scheduler("fluflu-scheduler")

      private object Worker extends Runnable {

        def start(): Either[Exception, Unit] =
          if (!(closed || worker.isShutdown)) {
            Right(worker.schedule(this, 5, NANOSECONDS))
          } else {
            Left(new Exception("Client executor was already shutdown"))
          }

        def run(): Unit = {
          def ignore = closed || queue.isEmpty
          if (ignore) {
            running.set(false)
          } else {
            consumer.consume()
            if (ignore) {
              running.set(false)
            } else {
              if (!(ignore || worker.isShutdown)) {
                worker.schedule(this, 5, NANOSECONDS)
              }
            }
          }
        }
      }

      def emit[A: Packer](tag: String, record: A, time: Instant): Either[Exception, Unit] =
        if (closed || worker.isShutdown)
          Left(new Exception("Client executor was already shutdown"))
        else {
          logger.trace(s"Queueing message: ${(tag, record, time)}")
          val fa = (p: MessageBufferPacker) => Packer[(A, Instant)].apply((record, time), p)
          if (queue.offer(tag -> fa))
            if (!running.get && running.compareAndSet(false, true)) Worker.start()
            else Right(())
          else
            Left(new Exception("The queue no space is currently available"))
        }

      def close(): Unit =
        try {
          closed = true
          awaitTermination(worker, 1.second)
          val closer = scheduler("fluflu-closer")
          closer.execute(new Runnable {
            def run(): Unit =
              while (!queue.isEmpty) {
                consumer.consume()
                NANOSECONDS.sleep(10)
              }
          })
          awaitTermination(closer, terminationTimeout)
        } finally {
          logger.info(s"Performed close the client. queue remaining: ${queue.size()}")
        }

      def size: Int = queue.size()

      def isClosed: Boolean = closed
    }
}
