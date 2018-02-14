package fluflu

import java.time.Instant

import fluflu.msgpack.Packer
import fluflu.msgpack.circe.MsgpackHelper
import io.circe.generic.auto._
import org.scalatest.FunSpec

class EventSpec extends FunSpec with MsgpackHelper {

  import Event._
  import fluflu.msgpack.circe._

  val packer: Packer[fluflu.Event[Map[String, String]]] = implicitly

  describe("Event") {
    it("should pack successfully") {
      val expected = Array(0x93, 0xa3, 0x61, 0x2e, 0x62, 0xce, 0x59, 0x68, 0x2f, 0x00, 0x81, 0xa1,
        0x6b, 0xa1, 0x76).map(_.toByte)
      val event      = Event("a", "b", Map("k" -> "v"), Instant.ofEpochSecond(1500000000L, 1L))
      val Right(arr) = packer(event)
      assert(arr === expected)
    }
  }

  describe("EventTime") {
    it("sould pack successfully") {
      val expected = Array(0x93, 0xa3, 0x61, 0x2e, 0x62, 0xd7, 0x00, 0x59, 0x68, 0x2f, 0x00, 0x00,
        0x00, 0x00, 0x01, 0x81, 0xa1, 0x6b, 0xa1, 0x76).map(_.toByte)
      val event      = EventTime("a", "b", Map("k" -> "v"), Instant.ofEpochSecond(1500000000L, 1L))
      val Right(arr) = packer(event)
      assert(arr === expected)
    }
  }
}
