package fluflu.msgpack

import fluflu.msgpack.json.MessagePackJson
import io.circe.Json

import scala.util.{ Either => \/ }

trait MessagePack[A] {
  def pack(a: A): Throwable \/ Array[Byte]
  def unpack(a: Array[Byte]): Option[A]
}

object MessagePack {

  def getInstance(i: Instances): MessagePack[Json] = i match {
    case JSON => MessagePackJson()
    // case MAP => ???
  }

  import java.nio.charset.StandardCharsets.UTF_8

  def formatArrayFamilyHeader(size: Int): Vector[Byte] =
    if (size < 16)
      Vector((0x90 | size).toByte)
    else if (size < 65536)
      Vector(0xdc, size >>> 8, size >>> 0).map(_.toByte)
    else
      Vector(0xdd, size >>> 24, size >>> 16, size >>> 8, size >>> 0).map(_.toByte)

  def formatMapFamilyHeader(sz: Int): Vector[Byte] =
    if (sz < 16)
      Vector((0x80 | sz).toByte)
    else if (sz < 65536)
      Vector(0xde, sz >>> 8, sz >>> 0).map(_.toByte)
    else
      Vector(0xdf, sz >>> 24, sz >>> 16, sz >>> 8, sz >>> 0).map(_.toByte)

  def formatStrFamilyHeader(sz: Int): Vector[Byte] =
    if (sz < 32)
      Vector((0xa0 | sz).toByte)
    else if (sz < 65536)
      Vector(0xda, sz >>> 8, sz >>> 0).map(_.toByte)
    else
      Vector(0xdb, sz >>> 24, sz >>> 16, sz >>> 8, sz >>> 0).map(_.toByte)

  def formatNil: Vector[Byte] = Vector(0xc0.toByte)

  def formatBoolFamily(v: Boolean): Vector[Byte] =
    if (v) Vector(0xc3.toByte) else Vector(0xc2.toByte)

  def formatIntFamily(l: Long): Vector[Byte] =
    if (4294967296L <= l) formatLong(0xcf, l)
    else if (65536L <= l) formatInt(0xce, l.toInt)
    else if (256L <= l) formatShort(0xcd, l.toInt)
    else if (128 <= l) formatByte(0xcc, l.toInt)
    else if (0 <= l) Vector(l.toByte)
    else if (l >= -32L) formatByte(0xe0 | (l + 32).toInt)
    else if (l >= Byte.MinValue.toLong) formatByte(0xd0, l.toInt)
    else if (l >= Short.MinValue.toLong) formatShort(0xd1, l.toInt)
    else if (l >= Int.MinValue.toLong) formatInt(0xd2, l.toInt)
    else formatLong(0xd3, l)
  def formatIntFamily(t: BigInt, v: BigInt): Vector[Byte] =
    Vector(t, v >> 56, v >> 48, v >> 40, v >> 32, v >> 24, v >> 16, v >> 8, v >> 0).map(_.toByte)

  import java.lang.Double.doubleToLongBits
  def formatFloatFamily(v: Double): Vector[Byte] =
    (doubleToLongBits _ andThen (x => Vector(0xcb.toLong, x >>> 56, x >>> 48, x >>> 40, x >>> 32, x >>> 24, x >>> 16, x >>> 8, x >>> 0)))(v).map(_.toByte)

  def formatStrFamily(v: String): Vector[Byte] =
    formatStrFamilyHeader(strSize(v.toCharArray)) ++ v.getBytes(UTF_8)

  def formatByte(v: Int): Vector[Byte] =
    Vector(v.toByte)
  def formatByte(t: Int, v: Int): Vector[Byte] =
    Vector(t, v).map(_.toByte)
  def formatShort(t: Int, v: Int): Vector[Byte] =
    Vector(t, v >>> 8, v >>> 0).map(_.toByte)
  def formatInt(t: Int, v: Int): Vector[Byte] =
    Vector(t, v >>> 24, v >>> 16, v >>> 8, v >>> 0).map(_.toByte)
  def formatLong(t: Long, v: Long): Vector[Byte] =
    Vector(t, v >>> 56, v >>> 48, v >>> 40, v >>> 32, v >>> 24, v >>> 16, v >>> 8, v >>> 0).map(_.toByte)

  def strSize(value: Array[Char]): Int = strSize(value, 0)
  def strSize(value: Array[Char], count: Int): Int = value match {
    case a if a.isEmpty => 0
    case Array(h) => count + charSize(h)
    case _ => strSize(value.tail, count + charSize(value(0)))
  }

  def charSize(ch: Char): Int =
    if (ch < 0x80) 1
    else if (ch < 0x800) 2
    else if (Character isHighSurrogate ch) 2
    else if (Character isLowSurrogate ch) 2
    else 3

}

sealed abstract class Instances(t: String)
object Instances {
  def of(t: String): Instances = t match {
    case "JSON" => JSON
    // case "MAP" => MAP
  }
}
case object JSON extends Instances("JSON")
// case object MAP extends Instances("MAP")

