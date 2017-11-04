package fluflu

import java.time.Instant

import cats.syntax.either._
import fluflu.msgpack.Packer
import io.circe.generic.auto._
import org.scalatest.{FunSpec, Matchers}

class EventSpec extends FunSpec with Matchers {

  import Event._
  import fluflu.msgpack.circe._

  val packer: Packer[fluflu.Event[Map[String, String]]] = implicitly

  describe("Event") {
    it("should pack successfully") {
      val expected = Seq(0x93, 0xa3, 0x61, 0x2e, 0x62, 0xce, 0x59, 0x68, 0x2f, 0x00, 0x81, 0xa1,
        0x6b, 0xa1, 0x76).map(_.toByte)
      val event = Event("a", "b", Map("k" -> "v"), Instant.ofEpochSecond(1500000000L, 1L))
      val arr   = packer(event).toTry.get
      assert(arr.toSeq === expected)
    }
  }

  describe("EventTime") {
    it("sould pack successfully") {
      val expected = Seq(0x93, 0xa3, 0x61, 0x2e, 0x62, 0xd7, 0x00, 0x59, 0x68, 0x2f, 0x00, 0x00,
        0x00, 0x00, 0x01, 0x81, 0xa1, 0x6b, 0xa1, 0x76).map(_.toByte)
      val event = EventTime("a", "b", Map("k" -> "v"), Instant.ofEpochSecond(1500000000L, 1L))
      val arr   = packer(event).toTry.get
      assert(arr.toSeq === expected)
    }
  }
}
