package fluflu

trait Backoff {
  def nextDelay(retries: Int): Long
}
