package fluflu.msgpack.shapes

import fluflu.msgpack.MsgpackHelper
import org.scalatest.FunSuite
import derivedCodecs._

class EncoderSpec extends FunSuite with MsgpackHelper {
  import MsgpackHelper._

  case class Bar(double: Double)
  case class Foo(int: Int, str: String, bar: Bar)

  def check[A](tc: Seq[(A, Array[Byte])])(implicit A: Encoder[A]): Unit = {
    for ((p, expected) <- tc) {
      packer.clear()
      Codec.serialize(A(p), packer)
      assert(packer.toByteArray === expected)
    }
  }

  test("Encoder[Some[Int]]") {
    check {
      Seq(
        (Some(1), x"01")
      )
    }
  }

  test("Encoder[Char]") {
    check {
      Seq(
        ('a', x"a1 61")
      )
    }
  }

  test("Encoder[None.type]") {
    check {
      Seq(
        (None, x"c0")
      )
    }
  }

  test("Encoder[Option[Int]]") {
    check {
      Seq(
        (Option(1), x"01"),
        (Option.empty[Int], x"c0")
      )
    }
  }

  test("Encoder[Boolean]") {
    check {
      Seq(
        (true, x"c3"),
        (false, x"c2")
      )
    }
  }

  test("Encoder[Byte]") {
    check {
      Seq(
        (0.toByte, x"00"),
        ((-32).toByte, x"e0"),
        ((-33).toByte, x"d0 df"),
        (Byte.MaxValue, x"7f"),
        (Byte.MinValue, x"d0 80")
      )
    }
  }

  test("Encoder[Short]") {
    check {
      Seq(
        (Byte.MaxValue.toShort, x"7f"),
        (Byte.MinValue.toShort, x"d0 80"),
        ((Byte.MaxValue.toShort + 1.toShort).toShort, x"cc 80"),
        ((Byte.MinValue.toShort - 1.toShort).toShort, x"d1 ff 7f"),
        (255.toShort, x"cc ff"),
        (256.toShort, x"cd 01 00"),
        (Short.MaxValue, x"cd 7f ff"),
        (Short.MinValue, x"d1 80 00")
      )
    }
  }

  test("Encoder[Int]") {
    check {
      Seq(
        (Short.MaxValue.toInt, x"cd 7f ff"),
        (Short.MinValue.toInt, x"d1 80 00"),
        (Short.MaxValue.toInt + 1, x"cd 80 00"),
        (65535, x"cd ff ff"),
        (65536, x"ce 00 01 00 00"),
        (Int.MaxValue, x"ce 7f ff ff ff"),
        (Int.MinValue, x"d2 80 00 00 00")
      )
    }
  }

  test("Encoder[Long]") {
    check {
      Seq(
        (Int.MaxValue.toLong, x"ce 7f ff ff ff"),
        (Int.MinValue.toLong, x"d2 80 00 00 00"),
        (Int.MinValue - 1L, x"d3 ff ff ff ff 7f ff ff ff"),
        (Int.MaxValue + 1L, x"ce 80 00 00 00"),
        (Long.MaxValue, x"cf 7f ff ff ff ff ff ff ff"),
        (Long.MinValue, x"d3 80 00 00 00 00 00 00 00")
      )
    }
  }

  test("Encoder[BigInt]") {
    check {
      Seq(
        (BigInt(Long.MaxValue), x"cf 7f ff ff ff ff ff ff ff"),
        (BigInt(Long.MinValue), x"d3 80 00 00 00 00 00 00 00"),
        ((BigInt(1) << 64) - 1, x"cf ff ff ff ff ff ff ff ff")
      )
    }
  }

  test("Encoder[Double]") {
    check {
      Seq(
        (0.0, x"cb 00 00 00 00 00 00 00 00"),
        (Double.MaxValue, x"cb 7f ef ff ff ff ff ff ff"),
        (Double.MinValue, x"cb ff ef ff ff ff ff ff ff")
      )
    }
  }

  test("Encoder[Float]") {
    check {
      Seq(
        (0.0f, x"ca 00 00 00 00"),
        (Float.MaxValue, x"ca 7f 7f ff ff"),
        (Float.MinValue, x"ca ff 7f ff ff")
      )
    }
  }

  test("Encoder[Seq[A]]") {
    check {
      Seq(
        (Seq(0 to 14: _*), x"9f 00 01 02 03 04 05 06 07 08 09 0a 0b 0c 0d 0e")
      )
    }
  }

  test("Encoder[List[A]]") {
    check {
      Seq(
        ((0 to 14).toList, x"9f 00 01 02 03 04 05 06 07 08 09 0a 0b 0c 0d 0e")
      )
    }
  }

  test("Encoder[Vector[A]]") {
    check {
      Seq(
        ((0 to 14).toVector, x"9f 00 01 02 03 04 05 06 07 08 09 0a 0b 0c 0d 0e")
      )
    }
  }

  test("Encoder[Map[A, B]]") {
    check {
      Seq(
        (('a' to 'z').zip(0 to 14).toMap,
         x"8f a1 64 03 a1 6f 0e a1 6b 0a a1 68 07 a1 63 02 a1 6c 0b a1 67 06 a1 62 01 a1 69 08 a1 6d 0c a1 61 00 a1 66 05 a1 6a 09 a1 6e 0d a1 65 04")
      )
    }
  }

  test("Encoder[Map[A, Bar]]") {
    check {
      Seq(
        (('a' to 'z').zip((0 to 14).map(a => Bar(a.toDouble))).toMap,
         x"8f a1 64 81 a6 64 6f 75 62 6c 65 cb 40 08 00 00 00 00 00 00 a1 6f 81 a6 64 6f 75 62 6c 65 cb 40 2c 00 00 00 00 00 00 a1 6b 81 a6 64 6f 75 62 6c 65 cb 40 24 00 00 00 00 00 00 a1 68 81 a6 64 6f 75 62 6c 65 cb 40 1c 00 00 00 00 00 00 a1 63 81 a6 64 6f 75 62 6c 65 cb 40 00 00 00 00 00 00 00 a1 6c 81 a6 64 6f 75 62 6c 65 cb 40 26 00 00 00 00 00 00 a1 67 81 a6 64 6f 75 62 6c 65 cb 40 18 00 00 00 00 00 00 a1 62 81 a6 64 6f 75 62 6c 65 cb 3f f0 00 00 00 00 00 00 a1 69 81 a6 64 6f 75 62 6c 65 cb 40 20 00 00 00 00 00 00 a1 6d 81 a6 64 6f 75 62 6c 65 cb 40 28 00 00 00 00 00 00 a1 61 81 a6 64 6f 75 62 6c 65 cb 00 00 00 00 00 00 00 00 a1 66 81 a6 64 6f 75 62 6c 65 cb 40 14 00 00 00 00 00 00 a1 6a 81 a6 64 6f 75 62 6c 65 cb 40 22 00 00 00 00 00 00 a1 6e 81 a6 64 6f 75 62 6c 65 cb 40 2a 00 00 00 00 00 00 a1 65 81 a6 64 6f 75 62 6c 65 cb 40 10 00 00 00 00 00 00")
      )
    }
  }
}
