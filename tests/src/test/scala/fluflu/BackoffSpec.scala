package fluflu

import java.time.Duration
import java.util.Random

import org.scalatest.FunSpec

class BackoffSpec extends FunSpec {
  class MyRandom extends Random {
    override def nextDouble: Double = 0.1
  }
  describe("exponential") {
    describe("nextDelay") {
      it("should return delay duration") {
        val backoff = Backoff.exponential(Duration.ofNanos(100), Duration.ofNanos(100), new MyRandom())
        assert(backoff.nextDelay().getNano === 10)
        assert(backoff.nextDelay().getNano === 20)
        assert(backoff.nextDelay().getNano === 40)
        assert(backoff.nextDelay().getNano === 80)
        assert(backoff.nextDelay().getNano === 100)
        assert(backoff.nextDelay().getNano === 100)
      }
    }
  }
}
