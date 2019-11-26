package fluflu

import scala.concurrent.duration._
import scala.util.Random

trait Backoff {
  def nextDelay(retries: Int): FiniteDuration
}

object Backoff {

  def fix(delay: FiniteDuration): Backoff = new Backoff {
    override def nextDelay(retries: Int): FiniteDuration = delay
  }

  def exponential(initialDelay: FiniteDuration, maximumDelay: FiniteDuration, random: Random): Backoff =
    new Backoff {

      override def nextDelay(retries: Int): FiniteDuration = {
        val next = (random.nextDouble * (initialDelay.toNanos << retries)).toLong.nanos
        if (next < maximumDelay) next else maximumDelay
      }
    }
}
