package fluflu
package msgpack

import java.lang.Double.doubleToLongBits
import java.nio.CharBuffer
import java.nio.charset.{CharsetEncoder, StandardCharsets}

import scala.collection.mutable

trait Packer[A] {
  def apply(a: A): Either[Throwable, Array[Byte]]
}

object Packer {

  private[this] val encoder: ThreadLocal[CharsetEncoder] =
    new ThreadLocal[CharsetEncoder] {
      override def initialValue(): CharsetEncoder =
        StandardCharsets.UTF_8.newEncoder()
    }

  def formatUInt32(v: Long, builder: mutable.ArrayBuilder[Byte]): Unit = {
    builder += (v >>> 24).toByte
    builder += (v >>> 16).toByte
    builder += (v >>> 8).toByte
    builder += (v >>> 0).toByte
  }

  def formatArrayFamilyHeader(size: Int, builder: mutable.ArrayBuilder[Byte]): Unit = {
    if (size < 16)
      builder += (0x90 | size).toByte
    else if (size < 65536) {
      builder += `0xdc`
      builder += (size >>> 8).toByte
      builder += (size >>> 0).toByte
    } else {
      builder += `0xdd`
      builder += (size >>> 24).toByte
      builder += (size >>> 16).toByte
      builder += (size >>> 8).toByte
      builder += (size >>> 0).toByte
    }
  }

  def formatMapFamilyHeader(sz: Int, builder: mutable.ArrayBuilder[Byte]): Unit = {
    if (sz < 16)
      builder += (0x80 | sz).toByte
    else if (sz < 65536) {
      builder += `0xde`
      builder += (sz >>> 8).toByte
      builder += (sz >>> 0).toByte
    } else {
      builder += `0xdf`
      builder += (sz >>> 24).toByte
      builder += (sz >>> 16).toByte
      builder += (sz >>> 8).toByte
      builder += (sz >>> 0).toByte
    }
  }

  def formatStrFamilyHeader(sz: Int, builder: mutable.ArrayBuilder[Byte]): Unit =
    if (sz < 32)
      builder += (0xa0 | sz).toByte
    else if (sz < 65536) {
      builder += `0xda`
      builder += (sz >>> 8).toByte
      builder += (sz >>> 0).toByte
    } else {
      builder += `0xdb`
      builder += (sz >>> 24).toByte
      builder += (sz >>> 16).toByte
      builder += (sz >>> 8).toByte
      builder += (sz >>> 0).toByte
    }

  def formatNil(builder: mutable.ArrayBuilder[Byte]): Unit = builder += `0xc0`

  def formatBoolFamily(v: Boolean, builder: mutable.ArrayBuilder[Byte]): Unit =
    builder += (if (v) `0xc3` else `0xc2`)

  def formatIntFamily(l: Long, builder: mutable.ArrayBuilder[Byte]): Unit =
    if (4294967296L <= l) formatLong(`0xcf`, l, builder)
    else if (65536L <= l) formatInt(`0xce`, l.toInt, builder)
    else if (256L <= l) formatShort(`0xcd`, l.toInt, builder)
    else if (128 <= l) formatByte(`0xcc`, l.toByte, builder)
    else if (0 <= l) formatByte(l.toByte, builder)
    else if (l >= -32L) formatByte((0xe0 | (l + 32)).toByte, builder)
    else if (l >= Byte.MinValue.toLong)
      formatByte(`0xd0`, l.toInt.toByte, builder)
    else if (l >= Short.MinValue.toLong) formatShort(`0xd1`, l.toInt, builder)
    else if (l >= Int.MinValue.toLong) formatInt(`0xd2`, l.toInt, builder)
    else formatLong(`0xd3`, l, builder)

  def formatIntFamily(t: Byte, v: BigInt, builder: mutable.ArrayBuilder[Byte]): Unit = {
    builder += t
    builder += (v >> 56).toByte
    builder += (v >> 48).toByte
    builder += (v >> 40).toByte
    builder += (v >> 32).toByte
    builder += (v >> 24).toByte
    builder += (v >> 16).toByte
    builder += (v >> 8).toByte
    builder += (v >> 0).toByte
  }

  def formatFloatFamily(v: Double, builder: mutable.ArrayBuilder[Byte]): Unit = {
    val x = doubleToLongBits(v)
    builder += `0xcb`
    builder += (x >>> 56).toByte
    builder += (x >>> 48).toByte
    builder += (x >>> 40).toByte
    builder += (x >>> 32).toByte
    builder += (x >>> 24).toByte
    builder += (x >>> 16).toByte
    builder += (x >>> 8).toByte
    builder += (x >>> 0).toByte
  }

  def formatStrFamily(v: String, builder: mutable.ArrayBuilder[Byte]): Unit = {
    val cb  = CharBuffer.wrap(v)
    val buf = encoder.get.encode(cb)
    val len = buf.remaining()
    formatStrFamilyHeader(len, builder)
    val arr = Array.ofDim[Byte](len)
    buf.get(arr)
    builder ++= arr
    buf.clear()
    cb.clear()
  }

  def formatByte(v: Byte, builder: mutable.ArrayBuilder[Byte]): Unit =
    builder += v

  def formatByte(t: Byte, v: Byte, builder: mutable.ArrayBuilder[Byte]): Unit = {
    builder += t
    builder += v
  }
  def formatShort(t: Byte, v: Int, builder: mutable.ArrayBuilder[Byte]): Unit = {
    builder += t
    builder += (v >>> 8).toByte
    builder += (v >>> 0).toByte
  }
  def formatInt(t: Byte, v: Int, builder: mutable.ArrayBuilder[Byte]): Unit = {
    builder += t
    builder += (v >>> 24).toByte
    builder += (v >>> 16).toByte
    builder += (v >>> 8).toByte
    builder += (v >>> 0).toByte
  }
  def formatLong(t: Byte, v: Long, builder: mutable.ArrayBuilder[Byte]): Unit = {
    builder += t
    builder += (v >>> 56).toByte
    builder += (v >>> 48).toByte
    builder += (v >>> 40).toByte
    builder += (v >>> 32).toByte
    builder += (v >>> 24).toByte
    builder += (v >>> 16).toByte
    builder += (v >>> 8).toByte
    builder += (v >>> 0).toByte
  }
}
