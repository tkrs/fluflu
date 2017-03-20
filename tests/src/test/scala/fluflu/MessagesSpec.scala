package fluflu

import org.scalatest.FunSpec

class MessagesSpec extends FunSpec {

  describe("pack") {
    it("should return a packed event") {
      val Right(result) = Messages.pack(Event("a", "b", Map("1" -> "xyz"), 1))
      val expected = Array(-109, -93, 97, 46, 98, 1, -127, -95, 49, -93, 120, 121, 122).map(_.toByte)
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
