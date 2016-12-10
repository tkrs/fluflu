package fluflu
package msgpack

import java.nio.ByteBuffer

import io.circe.{ Decoder, Encoder }
import org.scalacheck.{ Arbitrary, Prop, Shrink }
import org.scalatest.{ Assertion, FunSuite }
import org.scalatest.prop.Checkers

class MessagePackerUnpackerChecker extends FunSuite with Checkers {

  // implicit override val generatorDrivenConfig = PropertyCheckConfig(minSize = 10, maxSize = 30)

  implicit val arbBigInt: Arbitrary[BigInt] = Arbitrary(gen.genBigInt)

  def roundTrip[A: Encoder: Decoder: Arbitrary: Shrink]: Assertion =
    check(Prop.forAll({ a: A =>
      val packer = MessagePacker()
      val Right(x) = packer.encode(a)
      val unpacker = MessageUnpacker(ByteBuffer.wrap(x))
      val Right(b) = unpacker.decode[A]
      a === b
    }))

  test("Int")(roundTrip[Int])
  test("Long")(roundTrip[Long])
  test("Float")(roundTrip[Float])
  test("Double")(roundTrip[Double])
  test("String")(roundTrip[String])
  test("BigInt")(roundTrip[BigInt])
  test("Map[String, Int]")(roundTrip[Map[String, Int]])
  test("Map[String, Long]")(roundTrip[Map[String, Long]])
  test("Map[String, Float]")(roundTrip[Map[String, Float]])
  test("Map[String, Double]")(roundTrip[Map[String, Double]])
  test("Map[String, BigInt]")(roundTrip[Map[String, BigInt]])
  test("Map[String, String]")(roundTrip[Map[String, String]])
  test("Vector[Int]")(roundTrip[Vector[Int]])
  test("Vector[Long]")(roundTrip[Vector[Long]])
  test("Vector[Float]")(roundTrip[Vector[Float]])
  test("Vector[Double]")(roundTrip[Vector[Double]])
  test("Vector[BigInt]")(roundTrip[Vector[BigInt]])
  test("Vector[String]")(roundTrip[Vector[String]])
  test("List[Int]")(roundTrip[List[Int]])
  test("List[Long]")(roundTrip[List[Long]])
  test("List[Float]")(roundTrip[List[Float]])
  test("List[Double]")(roundTrip[List[Double]])
  test("List[BigInt]")(roundTrip[List[BigInt]])
  test("List[String]")(roundTrip[List[String]])
  test("Seq[Int]")(roundTrip[Seq[Int]])
  test("Seq[Long]")(roundTrip[Seq[Long]])
  test("Seq[Float]")(roundTrip[Seq[Float]])
  test("Seq[Double]")(roundTrip[Seq[Double]])
  test("Seq[BigInt]")(roundTrip[Seq[BigInt]])
  test("Seq[String]")(roundTrip[Seq[String]])
}