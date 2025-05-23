package fluflu

import java.time.Clock
import java.time.Instant
import java.time.ZoneId

import org.scalatest.funspec.AnyFunSpec
import org.scalatest.matchers.should.Matchers

import scala.concurrent.duration.*

class SleeperSpec extends AnyFunSpec with Matchers {
  describe("giveup") {
    it("should return false when it is called in time") {
      val clock   = Clock.fixed(Instant.now, ZoneId.systemDefault())
      val sleeper = Sleeper(Backoff.fix(10.millis), 10.millis, clock)
      assert(sleeper.giveUp === false)
    }
    it("should return true when it is timeout") {
      val clock   = Clock.systemUTC()
      val sleeper = Sleeper(Backoff.fix(10.millis), 20.millis, clock)
      sleeper.sleep(0)
      sleeper.sleep(0)
      sleeper.sleep(0)
      assert(sleeper.giveUp)
    }
  }
}
