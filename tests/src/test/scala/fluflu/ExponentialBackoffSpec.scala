package fluflu

import java.time.Duration
import java.util.Random

import org.scalatest.FunSpec

class ExponentialBackoffSpec extends FunSpec {
  class MyRandom extends Random {
    override def nextDouble: Double = 0.1
  }
  describe("nextDelay") {
    it("should return delay duration") {
      val backoff = ExponentialBackoff(Duration.ofNanos(100), Duration.ofNanos(100), new MyRandom())
      assert(backoff.nextDelay(0).getNano === 10)
      assert(backoff.nextDelay(1).getNano === 20)
      assert(backoff.nextDelay(2).getNano === 40)
      assert(backoff.nextDelay(3).getNano === 80)
      assert(backoff.nextDelay(4).getNano === 100)
      assert(backoff.nextDelay(5).getNano === 100)
    }
  }
}
