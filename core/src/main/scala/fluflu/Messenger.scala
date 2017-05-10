package fluflu

import java.nio.ByteBuffer
import java.time.{ Clock, Duration }

import cats.syntax.either._

import scala.annotation.tailrec

trait Messenger {
  def write(letter: Letter): Either[Throwable, Unit]
  def close(): Unit
}

object Messenger {

  def apply(timeout: Duration, backoff: Backoff)(
    implicit
    connection: Connection,
    clock: Clock
  ): Messenger =
    new MessengerImpl(timeout, backoff)

  final class MessengerImpl(
      timeout: Duration,
      backoff: Backoff
  )(implicit connection: Connection, clock: Clock) extends Messenger {

    def write(l: Letter): Either[Throwable, Unit] = {
      val buffer = Messages.getBuffer(l.message.length)
      buffer.put(l.message).flip()
      val r = write(buffer, 0, Sleeper(backoff, timeout, clock))
      buffer.clear()
      r
    }

    @tailrec private def write(buffer: ByteBuffer, retries: Int, sleeper: Sleeper): Either[Throwable, Unit] =
      connection.write(buffer) match {
        case Left(e) =>
          buffer.flip()
          if (sleeper.giveUp) e.asLeft else {
            sleeper.sleep(retries)
            write(buffer, retries + 1, sleeper)
          }
        case r =>
          if (!buffer.hasRemaining) r
          else write(buffer, retries, sleeper)
      }

    def close(): Unit = connection.close()
  }
}
