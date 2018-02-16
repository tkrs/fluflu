package fluflu.msgpack

import java.io.Closeable

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
}
