package fluflu.msgpack.time

import java.time.Instant

import fluflu.msgpack.MsgpackHelper
import fluflu.msgpack.Packer
import org.scalatest.funspec.AnyFunSpec

class IntegerSpec extends AnyFunSpec with MsgpackHelper {
  import MsgpackHelper._
  import fluflu.msgpack.time.integer._

  describe("Integer") {
    it("should pack successfully") {
      val expected = x"ce 59 68 2f 00"
      val instant  = Instant.ofEpochSecond(1500000000L, 1L)
      Packer[Instant].apply(instant, packer)
      assert(packer.toByteArray === expected)
    }
  }
}
