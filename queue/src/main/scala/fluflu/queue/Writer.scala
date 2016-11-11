package fluflu.queue

import java.nio.ByteBuffer
import java.time.{ Clock, Instant }
import java.util.concurrent._
import java.util.concurrent.atomic.AtomicReference

import cats.syntax.either._
import com.typesafe.scalalogging.LazyLogging
import fluflu.{ Event, Letter, Message, Messenger }
import io.circe.Encoder

import scala.util.{ Either => \/ }
import scala.compat.java8.FunctionConverters._

final case class Writer(
    messenger: Messenger,
    initialBufferSize: Int = 1024,
    initialDelay: Long = 0,
    delay: Long = 1,
    delayTimeUnit: TimeUnit = TimeUnit.SECONDS,
    terminationDelay: Long = 10,
    terminationDelayTimeUnit: TimeUnit = TimeUnit.SECONDS
)(implicit clock: Clock) extends LazyLogging {

  private[this] val letterQueue: BlockingDeque[() => Throwable \/ Letter] = new LinkedBlockingDeque()
  private[this] val scheduler = Executors.newScheduledThreadPool(1)

  private[this] val command: Runnable = new Runnable {
    private[this] val buffer =
      new AtomicReference[ByteBuffer](ByteBuffer.allocateDirect(initialBufferSize))
    override def run(): Unit = try {
      letterQueue.forEach(asJavaConsumer { fn =>
        fn().fold(
          e => logger.error(s"Failed to encode a message to message-pack", e),
          { letter =>
            val buf = buffer.updateAndGet(asJavaUnaryOperator { b =>
              if (b.limit >= letter.message.length) b
              else {
                if (b.capacity() < letter.message.length) {
                  b.clear()
                  ByteBuffer.allocateDirect(letter.message.length)
                } else {
                  b.limit(letter.message.length)
                  b
                }
              }
            })
            buf.put(letter.message).flip()
            messenger.write(buf, 0, Instant.now(clock)).fold(
              e => logger.error(s"Failed to send a message to remote: ${messenger.host}:${messenger.port}", e),
              _ => ()
            )
            buf.clear()
          }
        )
        letterQueue.remove(fn)
      })
    } finally {
      buffer.get.clear()
    }
  }

  private[this] val _: ScheduledFuture[_] =
    scheduler.scheduleWithFixedDelay(command, initialDelay, delay, delayTimeUnit)

  def push[A: Encoder](e: Event[A]): Exception \/ Unit =
    if (letterQueue offer (() => Message.pack(e).map(Letter))) Right(())
    else Left(new Exception("A queue no space is currently available"))

  def close(): Unit = {
    scheduler.shutdown()
    scheduler.awaitTermination(terminationDelay, terminationDelayTimeUnit)
    if (!scheduler.isTerminated) scheduler.shutdownNow()
    command.run()
    messenger.close()
  }
}
