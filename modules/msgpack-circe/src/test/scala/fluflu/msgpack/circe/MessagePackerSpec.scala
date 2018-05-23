package fluflu.msgpack.circe

import fluflu.msgpack.MsgpackHelper
import io.circe.Json
import io.circe.parser._
import org.scalatest._

class MessagePackerSpec extends WordSpec with MsgpackHelper {
  import MsgpackHelper._

  def instance = MessagePacker(packer)

  val fmt: Byte => String    = "0x%02x" format _
  val p: Array[Byte] => Unit = a => println(a.map(fmt).mkString(" "))

  "positive fixint" should {
    "be packed" in {
      val expected = x"7f"
      instance.pack(Json.fromInt(127))
      assert(packer.toByteArray === expected)
    }
  }

  "boolean false" should {
    "be packed" in {
      val expected = x"c2"
      instance.pack(Json.False)
      assert(packer.toByteArray === expected)
    }
  }

  "boolean true" should {
    "be packed" in {
      val expected = x"c3"
      instance.pack(Json.True)
      assert(packer.toByteArray === expected)
    }
  }

  "uint 8" should {
    "be packed" in {
      val expected = x"cc ff"
      instance.pack(Json.fromInt(255))
      assert(packer.toByteArray === expected)
    }
  }

  "uint 16" should {
    "be packed" in {
      val expected = x"cd 02 00"
      instance.pack(Json.fromInt(512))
      assert(packer.toByteArray === expected)
    }
  }

  "uint 32" should {
    "be packed" in {
      val expected = x"ce 00 01 00 00"
      instance.pack(Json.fromLong(65536))
      assert(packer.toByteArray === expected)
    }
  }

  "uint 64" should {
    "be packed" in {
      val expected = x"cf 00 00 00 01 00 00 00 00"
      instance.pack(Json.fromLong(4294967296L))
      assert(packer.toByteArray === expected)
    }

    "be packed by BigInt" in {
      val expected = x"cf ff ff ff ff ff ff ff fe"
      instance.pack(Json.fromBigInt(BigInt(Long.MaxValue) * 2))
      assert(packer.toByteArray === expected)
    }
  }

  "negative fixint" should {
    "be packed" in {
      val expected = x"ff"
      instance.pack(Json.fromInt(-1))
      assert(packer.toByteArray === expected)
    }
  }

  "negative int 8" should {
    "be packed" in {
      val expected = x"d0 df"
      instance.pack(Json.fromInt(-33))
      assert(packer.toByteArray === expected)
    }
  }

  "negative int 16" should {
    "be packed" in {
      val expected = x"d1 80 00"
      instance.pack(Json.fromInt(-32768))
      assert(packer.toByteArray === expected)
    }
  }

  "negative int 32" should {
    "be packed" in {
      val expected = x"d2 80 00 00 01"
      instance.pack(Json.fromLong(-2147483647))
      assert(packer.toByteArray === expected)
    }
  }

  "negative int 64" should {
    "be packed" in {
      val expected = x"d3 80 00 00 00 00 00 00 00"
      instance.pack(Json.fromLong(-9223372036854775808L))
      assert(packer.toByteArray === expected)
    }
  }

  "str 8" should {
    "be packed" in {
      val expected = x"a3 61 62 63"
      instance.pack(Json.fromString("abc"))
      assert(packer.toByteArray === expected)
    }
  }

  "str 16" should {
    "be packed" in {
      val expected =
        x"da 01 77 c2 a1 e2 84 a2 c2 a3 c2 a2 e2 88 9e c2 a7 c2 b6 e2 80 a2 c2 aa c2 ba e2 80 93 e2 89 a0 c5 93 e2 88 91 c2 b4 c2 ae e2 80 a0 c2 a5 c2 a8 cb 86 c3 b8 cf 80 e2 80 9c e2 80 98 c2 ab c3 a5 c3 9f e2 88 82 c6 92 c2 a9 cb 99 e2 88 86 cb 9a c2 ac e2 80 a6 c3 a6 ce a9 e2 89 88 c3 a7 e2 88 9a e2 88 ab cb 9c c2 b5 e2 89 a4 e2 89 a5 31 32 33 34 35 36 37 38 39 30 2d 3d 71 77 65 72 74 79 75 69 6f 70 5b 5d 61 73 64 66 67 68 6a 6b 6c 3b 27 7a 78 63 76 62 6e 6d 2c 2e 2f 7a 78 63 76 62 6e 6d 2c 2e 2e 2e 2e 2e 2e 2e 2e e3 81 82 e3 81 84 e3 81 86 e3 81 88 e3 81 8a e3 81 8b e3 81 8d e3 81 8f e3 81 91 e3 81 93 e3 81 95 e3 81 97 e3 81 99 e3 81 9b e3 81 9d c2 a1 c2 a1 e2 84 a2 c2 a3 c2 a2 e2 88 9e c2 a7 c2 b6 e2 80 a2 c2 aa c2 aa c2 aa c2 ba c2 ba c2 ba c2 ba c2 ba e2 80 93 e2 80 93 e2 80 93 e2 89 a0 c3 a5 c3 9f e2 88 82 e2 81 84 e2 82 ac e2 80 b9 e2 80 ba ef ac 81 ef ac 82 e2 80 a1 c2 b0 c2 b7 e2 80 9a e2 80 94 c5 92 e2 80 9e c2 b4 e2 80 b0 cb 87 c3 81 c2 a8 cb 86 c3 98 e2 88 8f e2 80 9d e2 80 99 c2 bb c3 85 c3 8d c3 8e c3 8f cb 9d c3 93 c3 94 ef a3 bf c3 92 c3 9a c2 b8 cb 9b c3 87 e2 97 8a c4 b1 cb 9c c3 82 c2 af cb 98 c3 86 c2 bf"
      instance
        .pack(Json.fromString(
          "¡™£¢∞§¶•ªº–≠œ∑´®†¥¨ˆøπ“‘«åß∂ƒ©˙∆˚¬…æΩ≈ç√∫˜µ≤≥1234567890-=qwertyuiop[]asdfghjkl;'zxcvbnm,./zxcvbnm,........あいうえおかきくけこさしすせそ¡¡™£¢∞§¶•ªªªººººº–––≠åß∂⁄€‹›ﬁﬂ‡°·‚—Œ„´‰ˇÁ¨ˆØ∏”’»ÅÍÎÏ˝ÓÔÒÚ¸˛Ç◊ı˜Â¯˘Æ¿"))
      assert(packer.toByteArray === expected)
    }
  }

  "fixarray" should {
    "be packed" in {
      val js         = """["", null, "あいうえお"]"""
      val Right(emp) = parse(js)
      val expected   = x"93 a0 c0 af e3 81 82 e3 81 84 e3 81 86 e3 81 88 e3 81 8a"
      instance.pack(emp)
      assert(packer.toByteArray === expected)
    }
  }

  "fixmap" should {
    "be packed" in {
      val js         = """{"name": "Taro"}"""
      val Right(emp) = parse(js)
      val expected   = x"81 a4 6e 61 6d 65 a4 54 61 72 6f"
      instance.pack(emp)
      assert(packer.toByteArray === expected)
    }
  }
}
