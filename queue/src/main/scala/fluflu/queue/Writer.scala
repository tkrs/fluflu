package fluflu.queue

import java.nio.ByteBuffer
import java.time.{ Clock, Instant }
import java.util.concurrent.{ BlockingDeque, Executors, LinkedBlockingDeque, TimeUnit }

import cats.data.Xor
import com.typesafe.scalalogging.LazyLogging
import fluflu.{ Event, Letter, Message, Messenger }
import io.circe.Encoder

import scala.annotation.tailrec
import scala.concurrent.blocking

final case class Writer(messenger: Messenger)(implicit clock: Clock) extends LazyLogging {

  private[this] val letterQueue: BlockingDeque[() => Throwable Xor Letter] = new LinkedBlockingDeque()
  private[this] val executor = Executors.newSingleThreadExecutor()

  private[this] val command: Runnable = new Runnable {
    private[this] val buffer = ByteBuffer.allocateDirect(1024)
    private[this] val blockingDuration: Long = 5000
    override def run(): Unit = {
      @tailrec def go(): Unit =
        if (executor.isShutdown) () else {
          Option(letterQueue.poll()) match {
            case None =>
              blocking(TimeUnit.NANOSECONDS.sleep(blockingDuration))
            case Some(f) =>
              f() match {
                case Xor.Left(e) => logger.error(s"Failed to pack a message", e)
                case Xor.Right(letter) =>
                  try {
                    if (buffer.limit < letter.message.length) buffer.limit(letter.message.length)
                    buffer.put(letter.message).flip()
                    messenger.write(buffer, 0, Instant.now(clock))
                  } catch {
                    case e: Throwable => logger.error(s"Failed to logging a message", e)
                  } finally {
                    buffer.clear()
                  }
              }
          }
          go()
        }
      go()
    }
  }

  executor.execute(command)

  def die: Boolean = messenger.die

  def push[A: Encoder](e: Event[A]): Unit =
    letterQueue offer (() => Message.pack(e).map(Letter))

  def close(): Unit = {
    executor.shutdown()
    executor.awaitTermination(10, TimeUnit.SECONDS)
    if (!executor.isTerminated) executor.shutdownNow()
    messenger.close()
  }
}
