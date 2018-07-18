package fluflu.msgpack.time

import java.time.Instant

import fluflu.msgpack.{MsgpackHelper, Packer}
import org.scalatest.FunSpec

class IntegerSpec extends FunSpec with MsgpackHelper {
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
