package fluflu.msgpack

import cats.data.Xor
import fluflu.msgpack.json.MessagePackJson

trait MessagePack[A] {
  def pack(a: A): Throwable Xor Array[Byte]
  def unpack(a: Array[Byte]): Option[A]
}

object MessagePack {

  def getInstance(i: Instances) = i match {
    case JSON => MessagePackJson()
    // case MAP => ???
  }

  import java.nio.charset.StandardCharsets.UTF_8

  def markArray(size: Int): Vector[Byte] =
    if (size < 16)
      Vector((0x90 | size).toByte)
    else if (size < 65536)
      Vector(0xdc, size >>> 8, size >>> 0).map(_.toByte)
    else
      Vector(0xdd, size >>> 24, size >>> 16, size >>> 8, size >>> 0).map(_.toByte)

  def markMap(sz: Int): Vector[Byte] =
    if (sz < 16)
      Vector((0x80 | sz).toByte)
    else if (sz < 65536)
      Vector(0xde, sz >>> 8, sz >>> 0).map(_.toByte)
    else
      Vector(0xdf, sz >>> 24, sz >>> 16, sz >>> 8, sz >>> 0).map(_.toByte)

  def markString(sz: Int): Vector[Byte] =
    if (sz < 32)
      Vector((0xa0 | sz).toByte)
    else if (sz < 65536)
      Vector(0xda, sz >>> 8, sz >>> 0).map(_.toByte)
    else
      Vector(0xdb, sz >>> 24, sz >>> 16, sz >>> 8, sz >>> 0).map(_.toByte)

  def nilFormat(): Vector[Byte] = Vector(0xc0.toByte)

  def boolFormat(v: Boolean): Vector[Byte] = v match {
    case true => Vector(0xc3.toByte)
    case false => Vector(0xc2.toByte)
  }

  def intFormat(l: Long): Vector[Byte] =
    if (Long.MaxValue < l) ???
    else if (4294967296L <= l) formatOfLong(0xcf, l)
    else if (65536L <= l) formatOfInt(0xce, l.toInt)
    else if (256L <= l) formatOfShort(0xcd, l.toInt)
    else if (0 <= l) formatOfByte(0xcc, l.toInt)
    else if (l >= -32L) formatOfByte(0xe0 | (l + 32).toInt)
    else if (l >= Byte.MinValue.toLong) formatOfByte(0xd0, l.toInt)
    else if (l >= Short.MinValue.toLong) formatOfShort(0xd1, l.toInt)
    else if (l >= Int.MinValue.toLong) formatOfInt(0xd2, l.toInt)
    else formatOfLong(0xd3, l)

  def floatFormat(v: Double): Vector[Byte] =
    if (Float.MinValue <= v && v <= Float.MaxValue)
      formatOfFloat(v.toFloat)
    else if (Double.MinValue <= v && v <= Double.MaxValue)
      formatOfDouble(v)
    else ???

  import java.lang.Float.floatToIntBits
  def formatOfFloat(v: Float): Vector[Byte] =
    (floatToIntBits _ andThen (x => Vector(0xca.toByte, (x >>> 24).toByte, (x >>> 16).toByte, (x >>> 8).toByte, (x >>> 0).toByte)))(v)

  import java.lang.Double.doubleToLongBits
  def formatOfDouble(v: Double): Vector[Byte] =
    (doubleToLongBits _ andThen (x => Vector(0xcb.toLong, x >>> 56, x >>> 48, x >>> 40, x >>> 32, x >>> 24, x >>> 16, x >>> 8, x >>> 0)))(v).map(_.toByte)

  def formatOfString(v: String): Vector[Byte] =
    markString(strSize(v.toCharArray)) ++ v.getBytes(UTF_8)

  def formatOfByte(v: Int): Vector[Byte] =
    Vector(v.toByte)
  def formatOfByte(t: Int, v: Int): Vector[Byte] =
    Vector(t, v).map(_.toByte)
  def formatOfShort(t: Int, v: Int): Vector[Byte] =
    Vector(t, v >>> 8, v >>> 0).map(_.toByte)
  def formatOfInt(t: Int, v: Int): Vector[Byte] =
    Vector(t, v >>> 24, v >>> 16, v >>> 8, v >>> 0).map(_.toByte)
  def formatOfLong(t: Long, v: Long): Vector[Byte] =
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
  def of(t: String) = t match {
    case "JSON" => JSON
    // case "MAP" => MAP
    case _ => ???
  }
}
case object JSON extends Instances("JSON")
// case object MAP extends Instances("MAP")

