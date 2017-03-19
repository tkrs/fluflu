package fluflu

import java.net.InetSocketAddress
import java.nio.ByteBuffer
import java.time.{ Clock, Duration, Instant }
import java.util.concurrent._

import scala.annotation.tailrec
import scala.concurrent.blocking
import scala.util.{ Either => \/ }

trait Messenger {
  def host: String
  def port: Int
  def write(letter: Letter): Throwable \/ Unit
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

  def write(l: Letter): Throwable \/ Unit = {
    val buffer = Messages.getBuffer(l.message.length)
    buffer.put(l.message).flip()
    val r = write(buffer, 0, Instant.now(clock))
    buffer.clear()
    r
  }

  @tailrec private def write(buffer: ByteBuffer, retries: Int, start: Instant): Throwable \/ Unit = {
    connection.write(buffer) match {
      case Left(e) =>
        buffer.flip()
        if (Instant.now(clock).minusNanos(rewriteTimeout.toNanos).compareTo(start) <= 0) {
          blocking(NANOSECONDS.sleep(rewriteBackoff.nextDelay(retries).toNanos))
          write(buffer, retries + 1, start)
        } else {
          Left(e)
        }
      case r => r
    }
  }

  def close(): Unit = connection.close()
}
