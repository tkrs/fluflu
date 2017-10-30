package fluflu.msgpack.circe

import cats.Eq
import cats.syntax.either._
import io.circe.Json
import io.circe.parser._
import org.msgpack.core.MessagePack
import org.scalatest._

class MessagePackerSpec extends WordSpec {

  implicit val arrayEq: Eq[Array[Byte]] = Eq.instance[Array[Byte]](_.zip(_).forall {
    case (a, b) => a == b
  })

  val instance = MessagePacker(MessagePack.DEFAULT_PACKER_CONFIG)

  val fmt: Byte => String    = "0x%02x" format _
  val p: Array[Byte] => Unit = a => println(a.map(fmt).mkString(" "))

  "positive fixint" should {
    "be packed" in {
      val expected = Array(0x7f).map(_.toByte)
      val a        = instance.pack(Json.fromInt(127)).toTry.get
      assert(a === expected)
    }
  }

  "uint 8" should {
    "be packed" in {
      val expected = Array(0xcc, 0xff).map(_.toByte)
      val a        = instance.pack(Json.fromInt(255)).toTry.get
      assert(a === expected)
    }
  }

  "uint 16" should {
    "be packed" in {
      val expected = Array(0xcd, 0x02, 0x00).map(_.toByte)
      val a        = instance.pack(Json.fromInt(512)).toTry.get
      assert(a === expected)
    }
  }

  "uint 32" should {
    "be packed" in {
      val expected = Array(0xce, 0x00, 0x01, 0x00, 0x00).map(_.toByte)
      val a        = instance.pack(Json.fromLong(65536)).toTry.get
      assert(a === expected)
    }
  }

  "uint 64" should {
    "be packed" in {
      val expected = Array(0xcf, 0x00, 0x00, 0x00, 0x01, 0x00, 0x00, 0x00, 0x00).map(_.toByte)
      val a        = instance.pack(Json.fromLong(4294967296L)).toTry.get
      assert(a === expected)
    }

    "be packed by BigInt" in {
      val expected = Array(0xcf, 0xff, 0xff, 0xff, 0xff, 0xff, 0xff, 0xff, 0xfe).map(_.toByte)
      val a        = instance.pack(Json.fromBigInt(BigInt(Long.MaxValue) * 2)).toTry.get
      assert(a === expected)
    }
  }

  "negative fixint" should {
    "be packed" in {
      val expected = Array(0xff).map(_.toByte)
      val a        = instance.pack(Json.fromInt(-1)).toTry.get
      assert(a === expected)
    }
  }

  "negative int 8" should {
    "be packed" in {
      val expected = Array(0xd0, 0xdf).map(_.toByte)
      val a        = instance.pack(Json.fromInt(-33)).toTry.get
      assert(a === expected)
    }
  }

  "negative int 16" should {
    "be packed" in {
      val expected = Array(0xd1, 0x80, 0x00).map(_.toByte)
      val a        = instance.pack(Json.fromInt(-32768)).toTry.get
      assert(a === expected)
    }
  }

  "negative int 32" should {
    "be packed" in {
      val expected = Array(0xd2, 0x80, 0x00, 0x00, 0x01).map(_.toByte)
      val a        = instance.pack(Json.fromLong(-2147483647)).toTry.get
      assert(a === expected)
    }
  }

  "negative int 64" should {
    "be packed" in {
      val expected = Array(0xd3, 0x80, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00).map(_.toByte)
      val a        = instance.pack(Json.fromLong(-9223372036854775808L)).toTry.get
      assert(a === expected)
    }
  }

  "str 8" should {
    "be packed" in {
      val expected = Array(0xa3, 0x61, 0x62, 0x63).map(_.toByte)
      val a        = instance.pack(Json.fromString("abc")).toTry.get
      assert(a === expected)
    }
  }

  "str 16" should {
    "be packed" in {
      val expected = Array(0xda, 0x01, 0x77, 0xc2, 0xa1, 0xe2, 0x84, 0xa2, 0xc2, 0xa3, 0xc2, 0xa2,
        0xe2, 0x88, 0x9e, 0xc2, 0xa7, 0xc2, 0xb6, 0xe2, 0x80, 0xa2, 0xc2, 0xaa, 0xc2, 0xba, 0xe2,
        0x80, 0x93, 0xe2, 0x89, 0xa0, 0xc5, 0x93, 0xe2, 0x88, 0x91, 0xc2, 0xb4, 0xc2, 0xae, 0xe2,
        0x80, 0xa0, 0xc2, 0xa5, 0xc2, 0xa8, 0xcb, 0x86, 0xc3, 0xb8, 0xcf, 0x80, 0xe2, 0x80, 0x9c,
        0xe2, 0x80, 0x98, 0xc2, 0xab, 0xc3, 0xa5, 0xc3, 0x9f, 0xe2, 0x88, 0x82, 0xc6, 0x92, 0xc2,
        0xa9, 0xcb, 0x99, 0xe2, 0x88, 0x86, 0xcb, 0x9a, 0xc2, 0xac, 0xe2, 0x80, 0xa6, 0xc3, 0xa6,
        0xce, 0xa9, 0xe2, 0x89, 0x88, 0xc3, 0xa7, 0xe2, 0x88, 0x9a, 0xe2, 0x88, 0xab, 0xcb, 0x9c,
        0xc2, 0xb5, 0xe2, 0x89, 0xa4, 0xe2, 0x89, 0xa5, 0x31, 0x32, 0x33, 0x34, 0x35, 0x36, 0x37,
        0x38, 0x39, 0x30, 0x2d, 0x3d, 0x71, 0x77, 0x65, 0x72, 0x74, 0x79, 0x75, 0x69, 0x6f, 0x70,
        0x5b, 0x5d, 0x61, 0x73, 0x64, 0x66, 0x67, 0x68, 0x6a, 0x6b, 0x6c, 0x3b, 0x27, 0x7a, 0x78,
        0x63, 0x76, 0x62, 0x6e, 0x6d, 0x2c, 0x2e, 0x2f, 0x7a, 0x78, 0x63, 0x76, 0x62, 0x6e, 0x6d,
        0x2c, 0x2e, 0x2e, 0x2e, 0x2e, 0x2e, 0x2e, 0x2e, 0x2e, 0xe3, 0x81, 0x82, 0xe3, 0x81, 0x84,
        0xe3, 0x81, 0x86, 0xe3, 0x81, 0x88, 0xe3, 0x81, 0x8a, 0xe3, 0x81, 0x8b, 0xe3, 0x81, 0x8d,
        0xe3, 0x81, 0x8f, 0xe3, 0x81, 0x91, 0xe3, 0x81, 0x93, 0xe3, 0x81, 0x95, 0xe3, 0x81, 0x97,
        0xe3, 0x81, 0x99, 0xe3, 0x81, 0x9b, 0xe3, 0x81, 0x9d, 0xc2, 0xa1, 0xc2, 0xa1, 0xe2, 0x84,
        0xa2, 0xc2, 0xa3, 0xc2, 0xa2, 0xe2, 0x88, 0x9e, 0xc2, 0xa7, 0xc2, 0xb6, 0xe2, 0x80, 0xa2,
        0xc2, 0xaa, 0xc2, 0xaa, 0xc2, 0xaa, 0xc2, 0xba, 0xc2, 0xba, 0xc2, 0xba, 0xc2, 0xba, 0xc2,
        0xba, 0xe2, 0x80, 0x93, 0xe2, 0x80, 0x93, 0xe2, 0x80, 0x93, 0xe2, 0x89, 0xa0, 0xc3, 0xa5,
        0xc3, 0x9f, 0xe2, 0x88, 0x82, 0xe2, 0x81, 0x84, 0xe2, 0x82, 0xac, 0xe2, 0x80, 0xb9, 0xe2,
        0x80, 0xba, 0xef, 0xac, 0x81, 0xef, 0xac, 0x82, 0xe2, 0x80, 0xa1, 0xc2, 0xb0, 0xc2, 0xb7,
        0xe2, 0x80, 0x9a, 0xe2, 0x80, 0x94, 0xc5, 0x92, 0xe2, 0x80, 0x9e, 0xc2, 0xb4, 0xe2, 0x80,
        0xb0, 0xcb, 0x87, 0xc3, 0x81, 0xc2, 0xa8, 0xcb, 0x86, 0xc3, 0x98, 0xe2, 0x88, 0x8f, 0xe2,
        0x80, 0x9d, 0xe2, 0x80, 0x99, 0xc2, 0xbb, 0xc3, 0x85, 0xc3, 0x8d, 0xc3, 0x8e, 0xc3, 0x8f,
        0xcb, 0x9d, 0xc3, 0x93, 0xc3, 0x94, 0xef, 0xa3, 0xbf, 0xc3, 0x92, 0xc3, 0x9a, 0xc2, 0xb8,
        0xcb, 0x9b, 0xc3, 0x87, 0xe2, 0x97, 0x8a, 0xc4, 0xb1, 0xcb, 0x9c, 0xc3, 0x82, 0xc2, 0xaf,
        0xcb, 0x98, 0xc3, 0x86, 0xc2, 0xbf).map(_.toByte)
      val a = instance
        .pack(Json.fromString(
          "¡™£¢∞§¶•ªº–≠œ∑´®†¥¨ˆøπ“‘«åß∂ƒ©˙∆˚¬…æΩ≈ç√∫˜µ≤≥1234567890-=qwertyuiop[]asdfghjkl;'zxcvbnm,./zxcvbnm,........あいうえおかきくけこさしすせそ¡¡™£¢∞§¶•ªªªººººº–––≠åß∂⁄€‹›ﬁﬂ‡°·‚—Œ„´‰ˇÁ¨ˆØ∏”’»ÅÍÎÏ˝ÓÔÒÚ¸˛Ç◊ı˜Â¯˘Æ¿"))
        .toTry
        .get
      assert(a === expected)
    }
  }

  "fixarray" should {
    "be packed" in {
      val js  = """["", null, "あいうえお"]"""
      val emp = parse(js).getOrElse(Json.Null)
      val expected =
        Array(0x93, 0xa0, 0xc0, 0xaf, 0xe3, 0x81, 0x82, 0xe3, 0x81, 0x84, 0xe3, 0x81, 0x86, 0xe3,
          0x81, 0x88, 0xe3, 0x81, 0x8a).map(_.toByte)
      val a = instance.pack(emp).toTry.get
      assert(a === expected)
    }
  }

  "fixmap" should {
    "be packed" in {
      val js  = """{"name": "Taro"}"""
      val emp = parse(js).getOrElse(Json.Null)
      val expected =
        Array(0x81, 0xa4, 0x6e, 0x61, 0x6d, 0x65, 0xa4, 0x54, 0x61, 0x72, 0x6f).map(_.toByte)
      val a = instance.pack(emp).toTry.get
      assert(a === expected)
    }
  }
}
