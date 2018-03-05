package fluflu.time

import java.time.Instant

import fluflu.msgpack.Packer
import org.scalatest.FunSpec

class IntegerSpec extends FunSpec {
  import fluflu.time.integer._

  describe("Integer") {
    it("should pack successfully") {
      val expected   = Array(0xce, 0x59, 0x68, 0x2f, 0x00).map(_.toByte)
      val instant    = Instant.ofEpochSecond(1500000000L, 1L)
      val Right(arr) = Packer[Instant].apply(instant)
      assert(arr === expected)
    }
  }

}
