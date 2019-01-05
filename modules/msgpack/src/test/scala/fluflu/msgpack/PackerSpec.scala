package fluflu.msgpack

import java.io.Closeable
import java.time.Instant

import org.msgpack.core.MessagePacker
import org.scalatest._

class PackerSpec extends FunSpec with MsgpackHelper {
  import MsgpackHelper._

  final class R extends Closeable {
    var closed        = false
    def close(): Unit = closed = true
  }

  object MyError extends Throwable

  describe("Packer[String]") {
    it("should be packed") {
      val expected = x"a5 61 62 63 65 64"
      Packer[String].apply("abced", packer)
      assert(packer.toByteArray === expected)
    }
  }

  describe("Packer[(String, A, Instant, Option[MOption)]") {
    it("should be packed") {
      val expected = x"93 a5 61 62 63 65 64 11 10"
      implicit val packInstant: Packer[Instant] = new Packer[Instant] {
        def apply(a: Instant, packer: MessagePacker): Unit =
          packer.packByte(0x11.toByte)
      }
      implicit val packMap: Packer[Map[String, String]] = new Packer[Map[String, String]] {
        def apply(a: Map[String, String], packer: MessagePacker): Unit =
          packer.packByte(0x10.toByte)
      }
      implicit val packMOption: Packer[MOption] = new Packer[MOption] {
        def apply(a: MOption, packer: MessagePacker): Unit = fail()
      }
      Packer[(String, Map[String, String], Instant, Option[MOption])]
        .apply(("abced", Map("a" -> "b"), Instant.ofEpochSecond(1500000000L, 1L), None), packer)
      assert(packer.toByteArray === expected)
    }
  }

  describe("Packer[(A, Instant)]") {
    it("should be packed") {
      val expected = x"92 11 10"
      implicit val packInstant: Packer[Instant] = new Packer[Instant] {
        def apply(a: Instant, packer: MessagePacker): Unit =
          packer.packByte(0x11.toByte)
      }
      implicit val packMap: Packer[Map[String, String]] = new Packer[Map[String, String]] {
        def apply(a: Map[String, String], packer: MessagePacker): Unit =
          packer.packByte(0x10.toByte)
      }
      Packer[(Map[String, String], Instant)]
        .apply((Map("a" -> "b"), Instant.ofEpochSecond(1500000000L, 1L)), packer)
      assert(packer.toByteArray === expected)
    }
  }
}
