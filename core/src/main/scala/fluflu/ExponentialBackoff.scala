package fluflu

import java.time.Duration

import scala.util.Random

final case class ExponentialBackoff(initialDelay: Duration, maximumDelay: Duration, random: Random) extends Backoff {
  def nextDelay(retries: Int): Duration = {
    val next = Duration.ofNanos((random.nextDouble * (initialDelay.toNanos << retries)).toLong)
    if (next.compareTo(maximumDelay) < 0) next else maximumDelay
  }
}
