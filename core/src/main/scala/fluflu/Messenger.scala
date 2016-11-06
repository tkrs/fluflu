package fluflu

import java.net.InetSocketAddress
import java.time.{ Clock, Duration, Instant }
import java.util.concurrent._

import scala.annotation.tailrec
import scala.concurrent.blocking
import scala.util.{ Failure, Success }

trait Messenger {
  def write(letter: Letter, retries: Int, start: Instant): Unit
  def die: Boolean
  def close(): Unit
}

final case class DefaultMessenger(
    host: String,
    port: Int,
    reconnectionTimeout: Duration,
    rewriteTimeout: Duration,
    reconnectionBackoff: Backoff,
    rewriteBackoff: Backoff
)(implicit clock: Clock) extends Messenger {

  private[this] val dest = new InetSocketAddress(host, port)
  private[this] val connection = Connection(dest, reconnectionTimeout, reconnectionBackoff)

  @tailrec def write(letter: Letter, retries: Int, start: Instant): Unit =
    connection.write(letter.message) match {
      case Failure(e) =>
        if (Instant.now(clock).minusNanos(rewriteTimeout.toNanos).compareTo(start) <= 0) {
          blocking {
            TimeUnit.NANOSECONDS.sleep(rewriteBackoff.nextDelay(retries).toNanos)
          }
          letter.message.flip()
          write(letter, retries + 1, start)
        } else {
          throw e
        }
      case Success(_) => ()
    }

  def die: Boolean = connection.noLongerRetriable

  def close(): Unit = {
    connection.close()
  }
}
