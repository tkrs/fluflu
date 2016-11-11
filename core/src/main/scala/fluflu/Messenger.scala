package fluflu

import java.net.InetSocketAddress
import java.nio.ByteBuffer
import java.time.{ Clock, Duration, Instant }
import java.util.concurrent._

import scala.annotation.tailrec
import scala.concurrent.blocking
import scala.util.{ Failure, Success }

trait Messenger {
  def write(buffer: ByteBuffer, retries: Int, start: Instant): Unit
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
  import TimeUnit._

  private[this] val dest = new InetSocketAddress(host, port)
  private[this] val connection = Connection(dest, reconnectionTimeout, reconnectionBackoff)

  @tailrec def write(buffer: ByteBuffer, retries: Int, start: Instant): Unit = {
    connection.write(buffer) match {
      case Failure(e) =>
        buffer.flip()
        if (Instant.now(clock).minusNanos(rewriteTimeout.toNanos).compareTo(start) <= 0) {
          blocking(NANOSECONDS.sleep(rewriteBackoff.nextDelay(retries).toNanos))
          write(buffer, retries + 1, start)
        } else {
          throw e
        }
      case Success(_) => ()
    }
  }

  def die: Boolean = connection.noLongerRetriable

  def close(): Unit = connection.close()
}
