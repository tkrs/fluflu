package fluflu

import java.time.Clock
import java.time.Instant

import scala.concurrent.blocking
import scala.concurrent.duration.*

trait Sleeper {
  def giveUp: Boolean
  def sleep(retries: Int): Unit
}

object Sleeper {
  def apply(backoff: Backoff, timeout: FiniteDuration, clock: Clock): Sleeper =
    new Sleeper {
      private val start = Instant.now(clock)

      def giveUp: Boolean =
        Instant.now(clock).minusNanos(timeout.toNanos).compareTo(start) > 0

      def sleep(retries: Int): Unit =
        blocking(NANOSECONDS.sleep(backoff.nextDelay(retries).toNanos))
    }
}
