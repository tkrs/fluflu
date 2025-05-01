package fluflu.msgpack

import java.io.Closeable
import java.time.Instant

import org.msgpack.core.MessagePacker
import org.scalatest.funspec.AnyFunSpec

class PackerSpec extends AnyFunSpec with MsgpackHelper {
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
