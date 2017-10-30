package fluflu
package msgpack

import java.nio.CharBuffer
import java.nio.charset.{CharsetEncoder, StandardCharsets}

import org.msgpack.core.MessagePack.Code

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
      builder += (Code.FIXSTR_PREFIX | sz).toByte
    else if (sz < 256) {
      builder += Code.STR8
      builder += sz.toByte
    } else if (sz < 65536) {
      builder += Code.STR16
      builder += (sz >>> 8).toByte
      builder += (sz >>> 0).toByte
    } else {
      builder += Code.STR32
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
}
