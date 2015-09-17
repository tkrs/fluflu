package fluflu.msgpack.json

import com.typesafe.scalalogging.LazyLogging
import fluflu.msgpack.Msgpack
import io.circe.Json

object MessagePackJson {
  def apply() = new MessagePackJson()
}

class MessagePackJson() extends Msgpack[Json] with LazyLogging {

  import java.nio.charset.StandardCharsets.UTF_8

  import io.circe.{ HCursor, Json }

  import scala.collection.mutable.ListBuffer

  override def pack(doc: Json): Seq[Byte] = {
    val buffer: ListBuffer[Byte] = ListBuffer.empty
    go(doc.hcursor, buffer)
    buffer.toSeq
  }

  private[this] def go(hc: HCursor, buffer: ListBuffer[Byte]): Unit =
    hc.focus match {
      case js if js.isArray =>
        js.asArray match {
          case None => // TODO: error?
          case Some(arr) =>
            buffer ++= beginSeq(arr.size)
            arr.foreach(e => go(e.hcursor, buffer))
        }
      case js if js.isObject => //
        js.asObject match {
          case None => // TODO: error?
          case Some(e) =>
            val l = e.toList
            beginObject(l.size).foreach(buffer += _)
            l.foreach {
              case (k, v) =>
                packString(k).foreach(buffer += _)
                go(v.hcursor, buffer)
            }
        }
      case js if js.isNull => buffer ++= packNull
      case js if js.isBoolean => js.asBoolean match {
        case None => // TODO: error?
        case Some(x) => buffer ++= packBool(x)
      }
      case js if js.isNumber =>
        val num = js.asNumber
        num match {
          case None => // TODO: error?
          case Some(x) =>
            val n = x.toBigDecimal
            if (n.isWhole())
              buffer ++= packNum(n.toLong)
            else
              buffer ++= packDouble(n.toDouble)
        }
      case js if js.isString =>
        js.asString match {
          case None => // TODO: error?
          case Some(s) => buffer ++= packString(s)
        }
      case _ => println(hc)
    }

  private[this] def beginSeq(size: Int): Seq[Byte] =
    if (size < 16)
      Seq((0x90 | size).toByte)
    else if (size < 65536)
      Seq(0xdc.toByte, (size >>> 8).toByte, (size >>> 0).toByte)
    else
      Seq(0xdd.toByte, (size >>> 24).toByte, (size >>> 16).toByte, (size >>> 8).toByte, (size >>> 0).toByte)

  private[this] def beginObject(sz: Int): Seq[Byte] =
    if (sz < 16)
      Seq((0x80 | sz).toByte)
    else if (sz < 65536)
      Seq(0xde.toByte, (sz >>> 8).toByte, (sz >>> 0).toByte)
    else
      Seq(0xdf.toByte, (sz >>> 24).toByte, (sz >>> 16).toByte, (sz >>> 8).toByte, (sz >>> 0).toByte)

  private[this] def beginString(sz: Int): Seq[Byte] = {
    if (sz < 32)
      Seq(0xa0 | sz)
    else if (sz < 65536)
      Seq(0xda, sz >>> 8, sz >>> 0)
    else
      Seq(0xdb, sz >>> 24, sz >>> 16, sz >>> 8, sz >>> 0)
  } map {
    _.toByte
  }

  private[this] def packNull(): Seq[Byte] = Seq(0xc0.toByte)

  private[this] def packBool(v: Boolean): Seq[Byte] = v match {
    case true => Seq(0xc3.toByte)
    case false => Seq(0xc2.toByte)
  }

  private[this] def packString(v: String): Seq[Byte] =
    beginString(estimateSizeUtf8(v.toCharArray, 0)) ++ v.getBytes(UTF_8)

  private[this] def packFloat(v: Float): Seq[Byte] = {
    val x = java.lang.Float.floatToIntBits(v)
    0xca.toByte :: ((x >>> 24) :: (x >>> 16) :: (x >>> 8) :: (x >>> 0) :: Nil).map(_.toByte)
  }

  private[this] def packDouble(v: Double): Seq[Byte] = {
    val x = java.lang.Double.doubleToLongBits(v)
    0xcb.toByte :: ((x >>> 56) :: (x >>> 48) :: (x >>> 40) :: (x >>> 32) :: (x >>> 24) :: (x >>> 16) :: (x >>> 8) :: (x >>> 0) :: Nil).map(_.toByte)
  }

  private[this] def packNum(l: Long): Seq[Byte] =
    if (l >= 0)
      if (l < 128L) packByte(l.toInt)
      else if (l < 256L) packByte(0xcc, l.toInt)
      else if (l < 65536L) packShort(0xcd, l.toInt)
      else if (l < 4294967296L) packInt(0xce, l.toInt)
      else if (l <= Long.MaxValue) packLong(0xcf, l)
      else ??? // unsigned is unsupported
    else if (l >= -32L) packByte(0xe0 | (l + 32).toInt)
    else if (l >= Byte.MinValue.toLong) packByte(0xd0, l.toInt)
    else if (l >= Short.MinValue.toLong) packShort(0xd1, l.toInt)
    else if (l >= Int.MinValue.toLong) packInt(0xd2, l.toInt)
    else packLong(0xd3, l)

  private[this] def packByte(v: Int): Seq[Byte] = Seq(v.toByte)
  private[this] def packByte(t: Int, v: Int): Seq[Byte] = Seq(t, v).map(_.toByte)
  private[this] def packShort(t: Int, v: Int): Seq[Byte] = Seq(t, v >>> 8, v >>> 0).map(_.toByte)
  private[this] def packInt(t: Int, v: Int): Seq[Byte] = Seq(t, v >>> 24, v >>> 16, v >>> 8, v >>> 0).map(_.toByte)
  private[this] def packLong(t: Long, v: Long): Seq[Byte] = Seq(t, v >>> 56, v >>> 48, v >>> 40, v >>> 32, v >>> 24, v >>> 16, v >>> 8, v >>> 0).map(_.toByte)

  private[this] def estimateSizeUtf8(value: Array[Char], count: Int): Int = value match {
    case Array(h) => count + charSize(h)
    case _ => estimateSizeUtf8(value.tail, count + charSize(value.head))
  }

  private[this] def charSize(ch: Char): Int =
    if (ch < 0x80) 1
    else if (ch < 0x800) 2
    else if (Character.isHighSurrogate(ch)) 2
    else if (Character.isLowSurrogate(ch)) 2
    else 3

  override def unpack(a: Seq[Byte]): Option[Json] = ???
}
