package fluflu

import java.time.Duration
import java.util.concurrent.atomic.AtomicInteger

import scala.util.Random

trait Backoff {
  def nextDelay(): Duration
}

object Backoff {

  def fix(delay: Duration): Backoff = new Backoff {
    def nextDelay(): Duration = delay
  }

  def exponential(
    initialDelay: Duration,
    maximumDelay: Duration,
    random: Random
  ): Backoff = new Backoff {
    private[this] val retries: AtomicInteger = new AtomicInteger(0)
    override def nextDelay(): Duration = {
      val next = Duration.ofNanos((random.nextDouble * (initialDelay.toNanos << retries.getAndIncrement)).toLong)
      if (next.compareTo(maximumDelay) < 0) next else maximumDelay
    }
  }
}
