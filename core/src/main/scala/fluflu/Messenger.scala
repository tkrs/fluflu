package fluflu

import java.io.IOException
import java.nio.ByteBuffer
import java.time.{Clock, Duration, Instant}

import monix.eval.Task

import scala.concurrent.duration._

trait Messenger {
  def write(letter: Letter): Task[Unit]
  def close(): Unit
}

object Messenger {

  def apply(timeout: Duration, backoff: Backoff)(implicit connection: Connection, clock: Clock): Messenger =
    new MessengerImpl(timeout, backoff)

  final class MessengerImpl(timeout: Duration, backoff: Backoff)(implicit connection: Connection, clock: Clock) extends Messenger {

    def write(l: Letter): Task[Unit] = {
      val buffer = ByteBuffer.wrap(l.message)
      write(buffer, 0, Instant.now(clock))
    }

    private def giveup(start: Instant): Boolean =
      Instant.now(clock).minusNanos(timeout.toNanos).compareTo(start) > 0

    private def write(buffer: ByteBuffer, retries: Int, start: Instant): Task[Unit] =
      connection
        .write(buffer)
        .flatMap { _ =>
          if (!buffer.hasRemaining) Task.unit
          else write(buffer, retries, start)
        }
        .onErrorRecoverWith {
          case e: IOException =>
            buffer.flip()
            if (giveup(start))
              Task.raiseError(e)
            else
              write(buffer, retries + 1, start)
                .delayExecution(backoff.nextDelay(retries).toNanos.nanos)
        }

    def close(): Unit = connection.close()
  }
}
