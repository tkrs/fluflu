package fluflu

import java.time.Duration

import scala.util.Random

trait Backoff {
  def nextDelay(retries: Int): Duration
}

object Backoff {

  def fix(delay: Duration): Backoff = new Backoff {

    override def nextDelay(retries: Int): Duration = delay
  }

  def exponential(initialDelay: Duration, maximumDelay: Duration, random: Random): Backoff = new Backoff {

    override def nextDelay(retries: Int): Duration = {
      val next = Duration.ofNanos((random.nextDouble * (initialDelay.toNanos << retries)).toLong)
      if (next.compareTo(maximumDelay) < 0) next else maximumDelay
    }
  }
}
