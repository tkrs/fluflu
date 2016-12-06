package fluflu

import java.net.InetSocketAddress
import java.time.{ Clock, Duration, Instant }

import cats.syntax.either._

import scala.annotation.tailrec
import scala.concurrent.blocking
import scala.util.{ Either => \/ }

abstract class Connection[A](
    rewriteTimeout: Duration,
    rewriteBackoff: Backoff
)(implicit clock: Clock) {
  def remote: InetSocketAddress
  def write(message: A): Throwable \/ Unit
  def close(): Unit

  import java.util.concurrent.TimeUnit._

  @tailrec final def write(msg: A, retries: Int, start: Instant): Throwable \/ Unit = {
    write(msg) match {
      case Left(e) =>
        if (Instant.now(clock).minusNanos(rewriteTimeout.toNanos).compareTo(start) <= 0) {
          blocking(NANOSECONDS.sleep(rewriteBackoff.nextDelay(retries).toNanos))
          write(msg, retries + 1, start)
        } else {
          \/.left(e)
        }
      case r => r
    }
  }
}
