package fluflu

import java.time.{ Clock, Duration, Instant }
import java.util.concurrent.TimeUnit

import scala.concurrent.blocking

trait Sleeper {
  def giveUp: Boolean
  def sleep(retries: Int): Unit
}

object Sleeper {
  import TimeUnit._

  def apply(backoff: Backoff, timeout: Duration, clock: Clock): Sleeper = new Sleeper {

    private[this] val start = Instant.now(clock)

    def giveUp: Boolean =
      Instant.now(clock).minusNanos(timeout.toNanos).compareTo(start) > 0

    def sleep(retries: Int): Unit =
      blocking(NANOSECONDS.sleep(backoff.nextDelay(retries).toNanos))
  }
}
