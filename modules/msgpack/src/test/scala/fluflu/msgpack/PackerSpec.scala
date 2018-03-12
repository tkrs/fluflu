package fluflu.msgpack

import java.io.Closeable
import java.time.Instant

import org.scalatest._

import scala.collection.mutable
import scala.util.{Failure, Try}

class PackerSpec extends FunSpec with MsgpackHelper {
  import MsgpackHelper._

  final class R extends Closeable {
    var closed        = false
    def close(): Unit = closed = true
  }

  object MyError extends Throwable

  describe("using") {
    it("should return fail when its passed resource creation was failed") {
      val r = Packer.using[R, Int](Failure(MyError))(_ => fail()).failed
      assert(r.get === MyError)
    }
    it("should close the resource automatically") {
      val r = Packer.using(Try(new R))(r => Try { assert(!r.closed); r }).get
      assert(r.closed)
    }
    it("should close the resource automatically when passed function occurs Exception") {
      val rr = new R
      Packer.using(Try(rr))(r => Try { assert(!r.closed); throw new Exception("Oops!") })
      assert(rr.closed)
    }
  }

  describe("formatStrFamilyHeader") {
    it("should create String's header correctly") {
      val table = Seq(
        31           -> x"bf",
        32           -> x"d9 20",
        255          -> x"d9 ff",
        256          -> x"da 01 00",
        65535        -> x"da ff ff",
        65536        -> x"db 00 01 00 00",
        Int.MaxValue -> x"db 7f ff ff ff"
      )

      for ((size, expected) <- table) {
        val builder: mutable.ArrayBuilder[Byte] = Array.newBuilder
        Packer.formatStrFamilyHeader(size, builder)
        assert(builder.result() === expected)
      }
    }
  }

  describe("packArrayHeader") {
    it("should create String's header correctly") {
      val table = Seq(
        1            -> x"91",
        15           -> x"9f",
        16           -> x"dc 00 10",
        65535        -> x"dc ff ff",
        65536        -> x"dd 00 01 00 00",
        Int.MaxValue -> x"dd 7f ff ff ff"
      )

      for ((size, expected) <- table) {
        val builder: mutable.ArrayBuilder[Byte] = Array.newBuilder
        Packer.formatArrayHeader(size, builder)
        assert(builder.result() === expected)
      }
    }
  }

  describe("Packer[String]") {
    it("should be packed") {
      val expected = x"a5 61 62 63 65 64"
      Packer[String].apply("abced") match {
        case Right(v) =>
          assert(v === expected)
        case Left(e) =>
          fail(e)
      }
    }
  }

  describe("Packer[(String, A, Instant)]") {
    it("should be packed") {
      val expected = x"93 a5 61 62 63 65 64 11 10"
      implicit val packInstant: Packer[Instant] = new Packer[Instant] {
        def apply(a: Instant): Either[Throwable, Array[Byte]] = Right(Array(0x11))
      }
      implicit val packMap: Packer[Map[String, String]] = new Packer[Map[String, String]] {
        def apply(a: Map[String, String]): Either[Throwable, Array[Byte]] = Right(Array(0x10))
      }
      Packer[(String, Map[String, String], Instant)]
        .apply(("abced", Map("a" -> "b"), Instant.ofEpochSecond(1500000000L, 1L))) match {
        case Right(v) =>
          assert(v === expected)
        case Left(e) =>
          fail(e)
      }
    }
  }

  describe("Packer[(A, Instant)]") {
    it("should be packed") {
      val expected = x"92 11 10"
      implicit val packInstant: Packer[Instant] = new Packer[Instant] {
        def apply(a: Instant): Either[Throwable, Array[Byte]] = Right(Array(0x11))
      }
      implicit val packMap: Packer[Map[String, String]] = new Packer[Map[String, String]] {
        def apply(a: Map[String, String]): Either[Throwable, Array[Byte]] = Right(Array(0x10))
      }
      Packer[(Map[String, String], Instant)]
        .apply((Map("a" -> "b"), Instant.ofEpochSecond(1500000000L, 1L))) match {
        case Right(v) =>
          assert(v === expected)
        case Left(e) =>
          fail(e)
      }
    }
  }
}
