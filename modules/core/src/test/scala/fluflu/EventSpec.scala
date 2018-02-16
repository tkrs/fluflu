package fluflu

import java.time.Instant

import fluflu.msgpack.{MsgpackHelper, Packer}
import org.scalatest.FunSpec

class EventSpec extends FunSpec with MsgpackHelper {

  implicit val packMapStringString: Packer[Map[String, String]] =
    new Packer[Map[String, String]] {
      def apply(a: Map[String, String]): Either[Throwable, Array[Byte]] =
        Right(Array.emptyByteArray)
    }

  describe("Event") {
    it("should pack successfully") {
      val expected =
        Array(0x93, 0xa3, 0x61, 0x2e, 0x62, 0xce, 0x59, 0x68, 0x2f, 0x00).map(_.toByte)
      val event      = Event("a", "b", Map.empty[String, String], Instant.ofEpochSecond(1500000000L, 1L))
      val Right(arr) = Packer[fluflu.Event[Map[String, String]]].apply(event)
      assert(arr === expected)
    }
  }

  describe("EventTime") {
    it("should pack successfully") {
      val expected = Array(0x93, 0xa3, 0x61, 0x2e, 0x62, 0xd7, 0x00, 0x59, 0x68, 0x2f, 0x00, 0x00,
        0x00, 0x00, 0x01).map(_.toByte)
      val event =
        Event.EventTime("a", "b", Map.empty[String, String], Instant.ofEpochSecond(1500000000L, 1L))
      val Right(arr) = Packer[fluflu.Event[Map[String, String]]].apply(event)
      assert(arr === expected)
    }
  }
}
