package fluflu.msgpack.circe

import java.nio.ByteBuffer
import java.nio.charset.StandardCharsets.UTF_8
import java.lang.Float.intBitsToFloat
import java.lang.Double.longBitsToDouble

import cats.syntax.either._
import io.circe.{Error, Decoder, Json, DecodingFailure}

import scala.annotation.tailrec

object MessageUnpacker {

  def apply(b: ByteBuffer) = new MessageUnpacker(b)

  def bytesToShort(buf: ByteBuffer, i: Int): Int =
    bytesToUShort(buf, i)

  def bytesToUShort(buf: ByteBuffer, i: Int): Int =
    ((buf.get(i) & 0xFF) << 8) |
      ((buf.get(i + 1) & 0xFF) << 0)

  def bytesToInt(buf: ByteBuffer, i: Int): Int =
    ((buf.get(i) & 0xFF) << 24) |
      ((buf.get(i + 1) & 0xFF) << 16) |
      ((buf.get(i + 2) & 0xFF) << 8) |
      ((buf.get(i + 3) & 0xFF) << 0)

  def bytesToUInt(buf: ByteBuffer, i: Int): Long =
    ((buf.get(i) & 0xFF).toLong << 24) |
      ((buf.get(i + 1) & 0xFF).toLong << 16) |
      ((buf.get(i + 2) & 0xFF).toLong << 8) |
      ((buf.get(i + 3) & 0xFF).toLong << 0)

  def bytesToLong(buf: ByteBuffer, i: Int): Long =
    ((buf.get(i) & 0xFF).toLong << 56) |
      ((buf.get(i + 1) & 0xFF).toLong << 48) |
      ((buf.get(i + 2) & 0xFF).toLong << 40) |
      ((buf.get(i + 3) & 0xFF).toLong << 32) |
      ((buf.get(i + 4) & 0xFF).toLong << 24) |
      ((buf.get(i + 5) & 0xFF).toLong << 16) |
      ((buf.get(i + 6) & 0xFF).toLong << 8) |
      ((buf.get(i + 7) & 0xFF).toLong << 0)

  def bytesToULong(buf: ByteBuffer, i: Int): BigInt =
    (BigInt(buf.get(i) & 0xFF) << 56) |
      (BigInt(buf.get(i + 1) & 0xFF) << 48) |
      (BigInt(buf.get(i + 2) & 0xFF) << 40) |
      (BigInt(buf.get(i + 3) & 0xFF) << 32) |
      (BigInt(buf.get(i + 4) & 0xFF) << 24) |
      (BigInt(buf.get(i + 5) & 0xFF) << 16) |
      (BigInt(buf.get(i + 6) & 0xFF) << 8) |
      (BigInt(buf.get(i + 7) & 0xFF) << 0)

  def utf8ToString(buf: ByteBuffer, offset: Int, length: Int): String = {
    val arr = Array.ofDim[Byte](length)
    buf.position(offset)
    buf.get(arr)
    new String(arr, UTF_8)
  }

}

final class MessageUnpacker(src: ByteBuffer) {
  import MessageUnpacker._

  private[this] var offset = src.position

  private def readAt(i: Int): Int = {
    val v = src.get(offset).toInt
    offset += i
    v
  }

  private def decodeAt[A](i: Int, f: (ByteBuffer, Int) => A): A = {
    val v = f(src, offset)
    offset += i
    v
  }

  def decode[A: Decoder]: Either[Error, A] =
    Either
      .catchOnly[Exception](unpack)
      .leftMap(e => DecodingFailure(e.getMessage, List.empty))
      .flatMap(_.as[A])

  def unpack: Json = readAt(1) & 0xff match {
    case 0xc0 => Json.Null
    case 0xc3 => Json.fromBoolean(true)
    case 0xc2 => Json.fromBoolean(false)
    case 0xca =>
      Json.fromDoubleOrNull(intBitsToFloat(decodeAt(4, bytesToInt)).toDouble)
    case 0xcb =>
      Json.fromDoubleOrNull(longBitsToDouble(decodeAt(8, bytesToLong)))
    case 0xd0                    => Json.fromInt(readAt(1))
    case 0xcc                    => Json.fromInt(readAt(1) & 0xff)
    case 0xd1                    => Json.fromInt(decodeAt(2, bytesToShort))
    case 0xcd                    => Json.fromInt(decodeAt(2, bytesToUShort))
    case 0xd2                    => Json.fromInt(decodeAt(4, bytesToInt))
    case 0xce                    => Json.fromLong(decodeAt(4, bytesToUInt))
    case 0xd3                    => Json.fromLong(decodeAt(8, bytesToLong))
    case 0xcf                    => Json.fromBigInt(decodeAt(8, bytesToULong))
    case 0xd9                    => unpackString(readAt(1) & 0xff)
    case 0xda                    => unpackString(decodeAt(2, bytesToShort))
    case 0xdb                    => unpackString(decodeAt(4, bytesToInt))
    case 0xdc                    => unpackList(decodeAt(2, bytesToShort))
    case 0xdd                    => unpackList(decodeAt(4, bytesToInt))
    case 0xde                    => unpackMap(decodeAt(2, bytesToShort))
    case 0xdf                    => unpackMap(decodeAt(4, bytesToInt))
    case t if (t & 0xe0) == 0xa0 => unpackString(t & 0x1f)
    case t if (t & 0xf0) == 0x80 => unpackMap(t & 0x0f)
    case t if (t & 0xf0) == 0x90 => unpackList(t & 0x0f)
    case t if t < 0x80           => Json.fromLong(t.toLong)
    case t if t >= 0xe0          => Json.fromLong((t - 0xe0 - 32).toLong)
    case t                       => throw new Exception("Unsupported type: 0x%02x".format(t))
  }

  private def unpackList(limit: Int): Json = {
    @tailrec def loop(i: Int, acc: Vector[Json]): Vector[Json] =
      if (i == limit) acc else loop(i + 1, acc :+ unpack)
    Json.fromValues(loop(0, Vector.empty))
  }

  private def unpackMap(size: Int): Json = {
    val key: Json => String = _.asString match {
      case Some(s) => s
      case None =>
        throw new Exception(s"Failed to decode key by the offset: $offset")
    }
    def loop(i: Int, acc: Vector[(String, Json)]): Vector[(String, Json)] =
      if (i == 0) acc else loop(i - 1, acc :+ (key(unpack) -> unpack))
    Json.fromFields(loop(size, Vector.empty))
  }

  private def unpackString(size: Int): Json =
    Json.fromString(decodeAt(size, utf8ToString(_, _, size)))
}
