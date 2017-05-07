package fluflu

import java.time.{ Clock, Duration, Instant }
import java.util.concurrent.TimeUnit

import scala.concurrent.blocking

trait Sleeper {
  def giveup: Boolean
  def sleep(): Unit
}

object Sleeper {
  import TimeUnit._

  def apply(backoff: Backoff, timeout: Duration, clock: Clock): Sleeper = new Sleeper {

    private[this] val start = Instant.now(clock)

    def giveup: Boolean =
      Instant.now(clock).minusNanos(timeout.toNanos).compareTo(start) > 0

    def sleep(): Unit =
      blocking(NANOSECONDS.sleep(backoff.nextDelay().toNanos))
  }
}
