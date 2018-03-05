package fluflu.time

import java.time.Instant

import fluflu.msgpack.Packer
import org.scalatest.FunSpec

class EventTimeSpec extends FunSpec {
  import eventTime._

  describe("EventTime") {
    it("should pack successfully") {
      val expected   = Array(0xd7, 0x00, 0x59, 0x68, 0x2f, 0x00, 0x00, 0x00, 0x00, 0x01).map(_.toByte)
      val instant    = Instant.ofEpochSecond(1500000000L, 1L)
      val Right(arr) = Packer[Instant].apply(instant)
      assert(arr === expected)
    }
  }
}
