package fluflu
package msgpack

import java.lang.Double.doubleToLongBits
import java.nio.{ ByteBuffer, CharBuffer }
import java.nio.charset.{ CharsetEncoder, StandardCharsets }

import cats.syntax.either._
import io.circe.{ Encoder, Json }

import scala.util.{ Either => \/ }

object MessagePacker {

  val encoder: CharsetEncoder = StandardCharsets.UTF_8.newEncoder()

  final val `0xc3`: Byte = 0xc3.toByte
  final val `0xc2`: Byte = 0xc2.toByte

  final val `0xcb`: Byte = 0xcb.toByte
  final val `0xcf`: Byte = 0xcf.toByte
  final val `0xce`: Byte = 0xce.toByte
  final val `0xcd`: Byte = 0xcd.toByte
  final val `0xcc`: Byte = 0xcc.toByte
  final val `0xd0`: Byte = 0xd0.toByte
  final val `0xd1`: Byte = 0xd1.toByte
  final val `0xd2`: Byte = 0xd2.toByte
  final val `0xd3`: Byte = 0xd3.toByte
  final val `0x3d`: Byte = 0xd3.toByte

  final val `0xda`: Byte = 0xda.toByte
  final val `0xdb`: Byte = 0xdb.toByte
  final val `0xdd`: Byte = 0xdd.toByte

  // final val `0x90`: Byte = 0x90.toByte
  final val `0xdc`: Byte = 0xdc.toByte
  final val `0xde`: Byte = 0xde.toByte
  final val `0xdf`: Byte = 0xdf.toByte

  def apply() = new MessagePacker()

  def formatArrayFamilyHeader(size: Int, builder: ByteBuffer): Unit = {
    if (size < 16)
      builder.put((0x90 | size).toByte)
    else if (size < 65536) {
      builder.put(`0xdc`)
      builder.put((size >>> 8).toByte)
      builder.put((size >>> 0).toByte)
    } else {
      builder.put(`0xdd`)
      builder.put((size >>> 24).toByte)
      builder.put((size >>> 16).toByte)
      builder.put((size >>> 8).toByte)
      builder.put((size >>> 0).toByte)
    }
  }

  def formatMapFamilyHeader(sz: Int, builder: ByteBuffer): Unit = {
    if (sz < 16)
      builder.put((0x80 | sz).toByte)
    else if (sz < 65536) {
      builder.put(`0xde`)
      builder.put((sz >>> 8).toByte)
      builder.put((sz >>> 0).toByte)
    } else {
      builder.put(`0xdf`)
      builder.put((sz >>> 24).toByte)
      builder.put((sz >>> 16).toByte)
      builder.put((sz >>> 8).toByte)
      builder.put((sz >>> 0).toByte)
    }
  }

  def formatStrFamilyHeader(sz: Int, builder: ByteBuffer): Unit =
    if (sz < 32)
      builder.put((0xa0 | sz).toByte)
    else if (sz < 65536) {
      builder.put(`0xda`)
      builder.put((sz >>> 8).toByte)
      builder.put((sz >>> 0).toByte)
    } else {
      builder.put(`0xdb`)
      builder.put((sz >>> 24).toByte)
      builder.put((sz >>> 16).toByte)
      builder.put((sz >>> 8).toByte)
      builder.put((sz >>> 0).toByte)
    }

  val formatNil: Byte = 0xc0.toByte

  def formatBoolFamily(v: Boolean, builder: ByteBuffer): Unit =
    builder.put(if (v) `0xc3` else `0xc2`)

  def formatIntFamily(l: Long, builder: ByteBuffer): Unit =
    if (4294967296L <= l) formatLong(`0xcf`, l, builder)
    else if (65536L <= l) formatInt(`0xce`, l.toInt, builder)
    else if (256L <= l) formatShort(`0xcd`, l.toInt, builder)
    else if (128 <= l) formatByte(`0xcc`, l.toByte, builder)
    else if (0 <= l) formatByte(l.toByte, builder)
    else if (l >= -32L) formatByte((0xe0 | (l + 32)).toByte, builder)
    else if (l >= Byte.MinValue.toLong) formatByte(`0xd0`, l.toInt.toByte, builder)
    else if (l >= Short.MinValue.toLong) formatShort(`0xd1`, l.toInt, builder)
    else if (l >= Int.MinValue.toLong) formatInt(`0xd2`, l.toInt, builder)
    else formatLong(`0xd3`, l, builder)

  def formatIntFamily(t: Byte, v: BigInt, builder: ByteBuffer): Unit = {
    builder.put(t)
    builder.put((v >> 56).toByte)
    builder.put((v >> 48).toByte)
    builder.put((v >> 40).toByte)
    builder.put((v >> 32).toByte)
    builder.put((v >> 24).toByte)
    builder.put((v >> 16).toByte)
    builder.put((v >> 8).toByte)
    builder.put((v >> 0).toByte)
  }

  def formatFloatFamily(v: Double, builder: ByteBuffer): Unit = {
    val x = doubleToLongBits(v)
    builder.put(`0xcb`)
    builder.put((x >>> 56).toByte)
    builder.put((x >>> 48).toByte)
    builder.put((x >>> 40).toByte)
    builder.put((x >>> 32).toByte)
    builder.put((x >>> 24).toByte)
    builder.put((x >>> 16).toByte)
    builder.put((x >>> 8).toByte)
    builder.put((x >>> 0).toByte)
  }

  def formatStrFamily(v: String, builder: ByteBuffer): Unit = {
    val cb = CharBuffer.wrap(v)
    val buf = encoder.encode(cb)
    formatStrFamilyHeader(strSize(cb), builder)
    builder.put(buf)
    cb.clear()
  }

  def formatByte(v: Byte, builder: ByteBuffer): Unit = builder.put(v)

  def formatByte(t: Byte, v: Byte, builder: ByteBuffer): Unit = {
    builder.put(t)
    builder.put(v)
  }
  def formatShort(t: Byte, v: Int, builder: ByteBuffer): Unit = {
    builder.put(t)
    builder.put((v >>> 8).toByte)
    builder.put((v >>> 0).toByte)
  }
  def formatInt(t: Byte, v: Int, builder: ByteBuffer): Unit = {
    builder.put(t)
    builder.put((v >>> 24).toByte)
    builder.put((v >>> 16).toByte)
    builder.put((v >>> 8).toByte)
    builder.put((v >>> 0).toByte)
  }
  def formatLong(t: Byte, v: Long, builder: ByteBuffer): Unit = {
    builder.put(t)
    builder.put((v >>> 56).toByte)
    builder.put((v >>> 48).toByte)
    builder.put((v >>> 40).toByte)
    builder.put((v >>> 32).toByte)
    builder.put((v >>> 24).toByte)
    builder.put((v >>> 16).toByte)
    builder.put((v >>> 8).toByte)
    builder.put((v >>> 0).toByte)
  }

  def strSize(cb: CharBuffer): Int =
    (0 until cb.capacity()).foldLeft(0)((l, r) => l + charSize(cb.get(r)))

  def charSize(ch: Char): Int =
    if (ch < 0x80) 1
    else if (ch < 0x800) 2
    else if (Character isHighSurrogate ch) 2
    else if (Character isLowSurrogate ch) 2
    else 3

}

final class MessagePacker {
  import MessagePacker._

  def encode[A](a: A)(implicit A: Encoder[A]): Throwable \/ Array[Byte] = pack(A(a))

  def pack(doc: Json): Throwable \/ Array[Byte] = {
    val acc: ByteBuffer = ByteBuffer.allocate(1048576)
    \/.catchNonFatal {
      go(doc, acc)
      val i = acc.position()
      val arr = Array.ofDim[Byte](i)
      acc.flip()
      acc.get(arr)
      acc.clear()
      arr
    }
  }

  def double(x: BigDecimal): Boolean = x.scale != 0

  def go(json: Json, acc: ByteBuffer): Unit =
    if (json.isNull)
      acc.put(formatNil)
    else if (json.isBoolean) json.asBoolean match {
      case None => ()
      case Some(x) => formatBoolFamily(x, acc)
    }
    else if (json.isNumber) json.asNumber match {
      case None => ()
      case Some(x) =>
        val n = x.toBigDecimal
        n match {
          case None => ()
          case Some(v) if double(v) =>
            formatFloatFamily(v.toDouble, acc)
          case Some(v) if v.isValidLong =>
            formatIntFamily(v.toLong, acc)
          case Some(v) if v.signum == -1 =>
            formatIntFamily(`0x3d`, v.toBigInt(), acc)
          case Some(v) =>
            formatIntFamily(`0xcf`, v.toBigInt(), acc)
        }
    }
    else if (json.isString) json.asString match {
      case None => ()
      case Some(x) =>
        formatStrFamily(x, acc)
    }
    else if (json.isArray) json.asArray match {
      case None => ()
      case Some(xs) =>
        formatArrayFamilyHeader(xs.size, acc)
        xs.foreach(go(_, acc))
    }
    else if (json.isObject) json.asObject match {
      case None => ()
      case Some(x) =>
        val xs = x.toList
        formatMapFamilyHeader(xs.size, acc)
        xs.foreach {
          case (key, (v)) =>
            formatStrFamily(key, acc)
            go(v, acc)
        }
    }
}
