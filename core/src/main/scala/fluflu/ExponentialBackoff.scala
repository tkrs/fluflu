package fluflu

import scala.util.Random

final case class ExponentialBackoff(initialDelayNanos: Long, random: Random) extends Backoff {
  def nextDelay(retries: Int): Long =
    (random.nextDouble * (initialDelayNanos << retries)).toLong / 1000000
}
