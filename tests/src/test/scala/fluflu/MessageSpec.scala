package fluflu

import org.scalatest.FunSpec

class MessageSpec extends FunSpec {

  describe("pack") {
    it("should return a packed event") {
      val Right(result) = Message.pack(Event("a", "b", Map("1" -> "xyz"), 1))
      val expected = Array(-109, -93, 97, 46, 98, 1, -127, -95, 49, -93, 120, 121, 122).map(_.toByte)
      assert(result === expected)
    }
  }
}
