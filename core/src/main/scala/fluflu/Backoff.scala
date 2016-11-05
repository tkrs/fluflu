package fluflu

import java.time.Duration

trait Backoff {
  def nextDelay(retries: Int): Duration
}
