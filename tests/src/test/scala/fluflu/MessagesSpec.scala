package fluflu

import java.time.Instant

import org.scalatest.FunSpec

class MessagesSpec extends FunSpec {
  import Event._

  describe("pack") {
    it("should return a packed event") {
      val time = Instant.parse("2007-12-03T10:15:30.12Z")
      val Right(result) = Messages.pack(Event("a", "b", Map("1" -> "xyz"), time))
      val expected = Array(0x93, 0xa3, 0x61, 0x2e, 0x62, 0xce, 0x47, 0x53, 0xd7, 0x42, 0x81, 0xa1, 0x31, 0xa3, 0x78, 0x79, 0x7a).map(_.toByte)
      assert(result === expected)
    }
    it("should return a packed event with nanosecond when it is used EventTime") {
      val time = Instant.parse("2007-12-03T10:15:30.12Z")
      val Right(result) = Messages.pack(EventTime("a", "b", Map("1" -> "xyz"), time))
      val expected = Array(0x93, 0xa3, 0x61, 0x2e, 0x62, 0xd7, 0x00, 0x47, 0x53, 0xd7, 0x42, 0x07, 0x27, 0x0e, 0x00, 0x81, 0xa1, 0x31, 0xa3, 0x78, 0x79, 0x7a).map(_.toByte)
      assert(result === expected)
    }
  }

  describe("getBuffer") {
    it("should return a ByteBuffer with increased capacity if it passed a size that exceeds capacity") {
      val buf = Messages.getBuffer(10)
      assert(buf.capacity() >= 10)

      val buf2 = Messages.getBuffer(buf.capacity() + 1)
      assert(buf.capacity() + 1 === buf2.capacity())
    }
  }
}
