package fluflu.msgpack

import java.nio.ByteBuffer

import cats.syntax.either._
import io.circe.generic.auto._
import org.scalatest.FunSpec

class TestDataTest extends FunSpec {
  import models._

  val test = new TestData
  import test._

  describe("`fixtype`") {

    it("should round trip") {
      val x = packer.encode(pack.fixType).toTry.get
      val t = unpacker(ByteBuffer.wrap(x)).decode[Prof].toTry.get
      assert(t === pack.fixType)
    }
  }
  describe("`int max 10`") {

    it("should round trip") {
      val x = packer.encode(pack.`int max 10`).toTry.get
      val t = unpacker(ByteBuffer.wrap(x)).decode[Int10].toTry.get
      assert(t === pack.`int max 10`)
    }
  }
  describe("`long max 10`") {

    it("should round trip") {
      val x = packer.encode(pack.`long max 10`).toTry.get
      val t = unpacker(ByteBuffer.wrap(x)).decode[Long10].toTry.get
      assert(t === pack.`long max 10`)
    }
  }
  describe("`string 100 10`") {

    it("should round trip") {
      val x = packer.encode(pack.`string 100 10`).toTry.get
      val t = unpacker(ByteBuffer.wrap(x)).decode[String10].toTry.get
      assert(t === pack.`string 100 10`)
    }
  }
  describe("`int max 30`") {

    it("should round trip") {
      val x = packer.encode(pack.`int max 30`).toTry.get
      val t = unpacker(ByteBuffer.wrap(x)).decode[Int30].toTry.get
      assert(t === pack.`int max 30`)
    }
  }
  describe("`long max 30`") {

    it("should round trip") {
      val x = packer.encode(pack.`long max 30`).toTry.get
      val t = unpacker(ByteBuffer.wrap(x)).decode[Long30].toTry.get
      assert(t === pack.`long max 30`)
    }
  }
  describe("`string 100 30`") {

    it("should round trip") {
      val x = packer.encode(pack.`string 100 30`).toTry.get
      val t = unpacker(ByteBuffer.wrap(x)).decode[String30].toTry.get
      assert(t === pack.`string 100 30`)
    }
  }
  describe("`string 1000 30`") {

    it("should round trip") {
      val x = packer.encode(pack.`string 1000 30`).toTry.get
      val t = unpacker(ByteBuffer.wrap(x)).decode[String30].toTry.get
      assert(t === pack.`string 1000 30`)
    }
  }
  describe("`string 1000 30 multibyte`") {

    it("should round trip") {
      val x = packer.encode(pack.`string 1000 30 multibyte`).toTry.get
      val t = unpacker(ByteBuffer.wrap(x)).decode[String30].toTry.get
      assert(t === pack.`string 1000 30 multibyte`)
    }
  }
}
