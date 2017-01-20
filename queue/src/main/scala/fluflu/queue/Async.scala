package fluflu
package queue

import java.nio.ByteBuffer
import java.time.{ Clock, Duration, Instant }
import java.util.concurrent._

import cats.syntax.either._
import com.typesafe.scalalogging.LazyLogging
import io.circe.Encoder

import scala.util.{ Either => \/ }
import scala.compat.java8.FunctionConverters._

final case class Async(
    messenger: Messenger,
    initialBufferSize: Int = 1024,
    initialDelay: Duration = Duration.ofMillis(1),
    delay: Duration = Duration.ofSeconds(1),
    terminationDelay: Duration = Duration.ofSeconds(10)
)(implicit clock: Clock) extends LazyLogging {

  private[this] val letterQueue: BlockingDeque[() => Throwable \/ Letter] = new LinkedBlockingDeque()
  private[this] val scheduler = Executors.newScheduledThreadPool(1)

  private[this] val command: Runnable = new Runnable {
    private[this] var buffer = ByteBuffer.allocateDirect(initialBufferSize)

    final val updateByteBuffer: (Int, ByteBuffer) => ByteBuffer = { (len, b) =>
      if (b.limit >= len) b else {
        if (b.capacity() < len) {
          b.clear()
          ByteBuffer.allocateDirect(len)
        } else {
          b.limit(len)
          b
        }
      }
    }

    final val write: Letter => Unit = { l =>
      buffer = updateByteBuffer(l.message.length, buffer)
      buffer.put(l.message).flip()
      messenger.write(buffer, 0, Instant.now(clock)).fold(
        e => logger.error(s"Failed to send a message to remote: ${messenger.host}:${messenger.port}", e),
        _ => ()
      )
      buffer.clear()
    }

    final val writeForEach: (() => Throwable \/ Letter) => Unit = { fn =>
      letterQueue.remove(fn)
      fn().fold(logger.error(s"Failed to encode a message to message-pack", _), write)
    }

    override def run(): Unit = try {
      letterQueue.forEach(asJavaConsumer(writeForEach))
    } finally {
      buffer.clear()
    }
  }

  private[this] val _: ScheduledFuture[_] =
    scheduler.scheduleWithFixedDelay(command, initialDelay.toNanos, delay.toNanos, TimeUnit.NANOSECONDS)

  def size: Int = letterQueue.size

  def push[A: Encoder](e: Event[A]): Exception \/ Unit =
    if (letterQueue offer (() => Message.pack(e).map(Letter))) \/.right(())
    else \/.left(new Exception("A queue no space is currently available"))

  def close(): Unit = {
    scheduler.shutdown()
    scheduler.awaitTermination(terminationDelay.toNanos, TimeUnit.NANOSECONDS)
    if (!scheduler.isTerminated) scheduler.shutdownNow()
    if (!letterQueue.isEmpty) logger.debug(s"message queue has remaining: ${letterQueue.size()}")
    command.run()
    messenger.close()
  }
}
