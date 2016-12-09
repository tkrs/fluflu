package fluflu.msgpack

import io.circe.generic.auto._
import org.scalatest.FunSpec

class TestDataTest extends FunSpec {
  import models._

  val test = new TestData

  describe("pack") {
    import test.pack._
    describe("`int max 10`") {
      it("should decode") {
        val Right(x) = test.packer.encode(`int max 10`)
        assert(x.length > 0)
      }
    }
    describe("`long max 10`") {

      it("should decode") {
        val Right(x) = test.packer.encode(`long max 10`)
        assert(x.length > 0)
      }
    }
    describe("`string 100 10`") {

      it("should decode") {
        val Right(x) = test.packer.encode(`string 100 10`)
        assert(x.length > 0)
      }
    }
    describe("`int max 30`") {

      it("should decode") {
        val Right(x) = test.packer.encode(`int max 30`)
        assert(x.length > 0)
      }
    }
    describe("`long max 30`") {

      it("should decode") {
        val Right(x) = test.packer.encode(`long max 30`)
        assert(x.length > 0)
      }
    }
    describe("`string 100 30`") {

      it("should decode") {
        val Right(x) = test.packer.encode(`string 100 30`)
        assert(x.length > 0)
      }
    }
    describe("`string 1000 30`") {

      it("should decode") {
        val Right(x) = test.packer.encode(`string 1000 30`)
        assert(x.length > 0)
      }
    }
    describe("`string 1000 30 multibyte`") {

      it("should decode") {
        val Right(x) = test.packer.encode(`string 1000 30 multibyte`)
        assert(x.length > 0)
      }
    }

  }
  describe("unpack") {
    import test.unpack._
    describe("`int max 10`") {
      it("should decode") {
        val Right(x) = test.unpacker(`int max 10`).decode[Int10]
        assert(x === test.pack.`int max 10`)
      }
    }
    describe("`long max 10`") {

      it("should decode") {
        val Right(x) = test.unpacker(`long max 10`).decode[Long10]
        assert(x === test.pack.`long max 10`)
      }
    }
    describe("`string 100 10`") {

      it("should decode") {
        val Right(x) = test.unpacker(`string 100 10`).decode[String10]
        assert(x === test.pack.`string 100 10`)
      }
    }
    describe("`int max 30`") {

      it("should decode") {
        val Right(x) = test.unpacker(`int max 30`).decode[Int30]
        assert(x === test.pack.`int max 30`)
      }
    }
    describe("`long max 30`") {

      it("should decode") {
        val Right(x) = test.unpacker(`long max 30`).decode[Long30]
        assert(x === test.pack.`long max 30`)
      }
    }
    describe("`string 100 30`") {

      it("should decode") {
        val Right(x) = test.unpacker(`string 100 30`).decode[String30]
        assert(x === test.pack.`string 100 30`)
      }
    }
    describe("`string 1000 30`") {

      it("should decode") {
        val Right(x) = test.unpacker(`string 1000 30`).decode[String30]
        assert(x === test.pack.`string 1000 30`)
      }
    }
    describe("`string 1000 30 multibyte`") {

      it("should decode") {
        val Right(x) = test.unpacker(`string 1000 30 multibyte`).decode[String30]
        assert(x === test.pack.`string 1000 30 multibyte`)
      }
    }

  }
}
