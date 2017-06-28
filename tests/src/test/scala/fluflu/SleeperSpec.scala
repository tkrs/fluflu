package fluflu

import java.time.{ Clock, Duration, Instant, ZoneId }

import org.scalatest.{ FunSpec, Matchers }

class SleeperSpec extends FunSpec with Matchers {

  describe("giveup") {
    it("should return false when it is called in time") {
      val clock = Clock.fixed(Instant.now, ZoneId.systemDefault())
      val sleeper = Sleeper(Backoff.fix(Duration.ofMillis(10)), Duration.ofMillis(10), clock)
      assert(sleeper.giveUp === false)
    }
    it("should return true when it is timeout") {
      val clock = Clock.systemUTC()
      val sleeper = Sleeper(Backoff.fix(Duration.ofNanos(10)), Duration.ofNanos(20), clock)
      sleeper.sleep(0)
      sleeper.sleep(0)
      sleeper.sleep(0)
      assert(sleeper.giveUp)
    }
  }
}
