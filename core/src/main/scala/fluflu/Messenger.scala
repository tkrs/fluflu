package fluflu

import java.nio.ByteBuffer
import java.time.{ Clock, Duration }
import java.util.concurrent._

import cats.syntax.either._
import com.typesafe.scalalogging.LazyLogging

import scala.annotation.tailrec
import scala.util.Random

trait Messenger {
  def write(letter: Letter): Either[RejectedExecutionException, Unit]
  def close(): Unit
}

object Messenger {

  private[this] val rnd: Random = new Random(System.nanoTime())

  def apply(
    parallelism: Int = 1,
    timeout: Duration = Duration.ofMillis(10000),
    backoff: Backoff = Backoff.exponential(Duration.ofNanos(500), Duration.ofSeconds(5), rnd),
    terminationDelay: Duration = Duration.ofSeconds(10)
  )(implicit c: Connection, clock: Clock = Clock.systemUTC): Messenger =
    new Pool(parallelism, timeout, backoff, clock, terminationDelay)

  private[this] final class Pool(
      parallelism: Int,
      timeout: Duration,
      backoff: Backoff,
      clock: Clock,
      terminationDelay: Duration
  )(implicit connection: Connection) extends Messenger with LazyLogging {

    private[this] val pool = Executors.newFixedThreadPool(parallelism)

    def write(l: Letter): Either[RejectedExecutionException, Unit] = Either.catchOnly[RejectedExecutionException] {
      pool.execute(new Runnable {
        def run(): Unit = {
          val buffer = Messages.getBuffer(l.message.length)
          buffer.put(l.message).flip()
          write(buffer, Sleeper(backoff, timeout, clock)).fold(
            e => logger.error(s"Failed to send a message", e),
            _ => ()
          )
          buffer.clear()
        }
      })
    }

    @tailrec private def write(buffer: ByteBuffer, sleeper: Sleeper): Either[Throwable, Unit] = {
      connection.write(buffer) match {
        case Left(e) =>
          buffer.flip()
          if (sleeper.giveup) Left(e) else {
            sleeper.sleep()
            write(buffer, sleeper)
          }
        case _ =>
          if (buffer.hasRemaining) write(buffer, sleeper) else Right(())
      }
    }

    def close(): Unit = {
      awaitTermination(pool, terminationDelay)
      connection.close()
    }
  }
}
