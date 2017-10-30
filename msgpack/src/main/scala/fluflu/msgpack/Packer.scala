package fluflu
package msgpack

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
