package fluflu.time

import java.time.Instant

import fluflu.msgpack.{MsgpackHelper, Packer}
import org.scalatest.FunSpec

class EventTimeSpec extends FunSpec with MsgpackHelper {
  import MsgpackHelper._
  import eventTime._

  describe("EventTime") {
    it("should pack successfully") {
      val expected = x"d7 00 59 68 2f 00 00 00 00 01"
      val instant  = Instant.ofEpochSecond(1500000000L, 1L)
      Packer[Instant].apply(instant, packer)
      assert(packer.toByteArray === expected)
    }
  }
}
