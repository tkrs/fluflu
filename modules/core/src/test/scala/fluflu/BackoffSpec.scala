package fluflu

import java.util.Random

import org.scalatest.funspec.AnyFunSpec

import scala.concurrent.duration._

class BackoffSpec extends AnyFunSpec {
  class MyRandom extends Random {
    override def nextDouble: Double = 0.1
  }
  describe("exponential") {
    describe("nextDelay") {
      it("should return delay duration") {
        val backoff =
          Backoff.exponential(100.nanos, 100.nanos, new MyRandom)
        assert(backoff.nextDelay(0).toNanos === 10)
        assert(backoff.nextDelay(1).toNanos === 20)
        assert(backoff.nextDelay(2).toNanos === 40)
        assert(backoff.nextDelay(3).toNanos === 80)
        assert(backoff.nextDelay(4).toNanos === 100)
        assert(backoff.nextDelay(5).toNanos === 100)
      }
    }
  }
}
