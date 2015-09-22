package fluflu.msgpack

import fluflu.msgpack.json.MessagePackJson

trait MessagePack[A] {
  def pack(a: A): Array[Byte]
  def unpack(a: Array[Byte]): Option[A]
}

object MessagePack {

  def getInstance(i: Instances) = i match {
    case JSON => MessagePackJson()
    case MAP => ???
  }

  import java.nio.charset.StandardCharsets.UTF_8

  def markArray(size: Int): Seq[Byte] =
    if (size < 16)
      Seq((0x90 | size).toByte)
    else if (size < 65536)
      Seq(0xdc.toByte, (size >>> 8).toByte, (size >>> 0).toByte)
    else
      Seq(0xdd.toByte, (size >>> 24).toByte, (size >>> 16).toByte, (size >>> 8).toByte, (size >>> 0).toByte)

  def markMap(sz: Int): Seq[Byte] =
    if (sz < 16)
      Seq((0x80 | sz).toByte)
    else if (sz < 65536)
      Seq(0xde.toByte, (sz >>> 8).toByte, (sz >>> 0).toByte)
    else
      Seq(0xdf.toByte, (sz >>> 24).toByte, (sz >>> 16).toByte, (sz >>> 8).toByte, (sz >>> 0).toByte)

  def markString(sz: Int): Seq[Byte] =
    if (sz < 32)
      Seq((0xa0 | sz).toByte)
    else if (sz < 65536)
      Seq(0xda.toByte, (sz >>> 8).toByte, (sz >>> 0).toByte)
    else
      Seq(0xdb.toByte, (sz >>> 24).toByte, (sz >>> 16).toByte, (sz >>> 8).toByte, (sz >>> 0).toByte)

  def nilFormat(): Seq[Byte] = Seq(0xc0.toByte)

  def boolFormat(v: Boolean): Seq[Byte] = v match {
    case true => Seq(0xc3.toByte)
    case false => Seq(0xc2.toByte)
  }

  def intFormat(l: Long): Seq[Byte] =
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

  def floatFormat(v: Double): Seq[Byte] =
    if (Float.MinValue <= v && v <= Float.MaxValue)
      formatOfFloat(v.toFloat)
    else if (Double.MinValue <= v && v <= Double.MaxValue)
      formatOfDouble(v)
    else ???

  import java.lang.Float.floatToIntBits
  def formatOfFloat(v: Float): Seq[Byte] =
    (floatToIntBits _ andThen (x => Seq(0xca.toByte, (x >>> 24).toByte, (x >>> 16).toByte, (x >>> 8).toByte, (x >>> 0).toByte)))(v)

  import java.lang.Double.doubleToLongBits
  def formatOfDouble(v: Double): Seq[Byte] =
    (doubleToLongBits _ andThen (x => Seq(0xcb.toLong, x >>> 56, x >>> 48, x >>> 40, x >>> 32, x >>> 24, x >>> 16, x >>> 8, x >>> 0)))(v).map(_.toByte)

  def formatOfString(v: String): Seq[Byte] =
    markString(strSize(v.toCharArray)) ++ v.getBytes(UTF_8)

  def formatOfByte(v: Int): Seq[Byte] =
    Seq(v.toByte)
  def formatOfByte(t: Int, v: Int): Seq[Byte] =
    Seq(t.toByte, v.toByte)
  def formatOfShort(t: Int, v: Int): Seq[Byte] =
    Seq(t.toByte, (v >>> 8).toByte, (v >>> 0).toByte)
  def formatOfInt(t: Int, v: Int): Seq[Byte] =
    Seq(t.toByte, (v >>> 24).toByte, (v >>> 16).toByte, (v >>> 8).toByte, (v >>> 0).toByte)
  def formatOfLong(t: Long, v: Long): Seq[Byte] =
    Seq(t.toByte, (v >>> 56).toByte, (v >>> 48).toByte, (v >>> 40).toByte, (v >>> 32).toByte, (v >>> 24).toByte, (v >>> 16).toByte, (v >>> 8).toByte, (v >>> 0).toByte)

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
    case "MAP" => MAP
    case _ => ???
  }
}
case object JSON extends Instances("JSON")
case object MAP extends Instances("MAP")

