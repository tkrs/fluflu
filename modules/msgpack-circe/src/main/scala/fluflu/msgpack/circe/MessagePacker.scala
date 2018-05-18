package fluflu.msgpack
package circe

import io.circe.{Encoder, Json, JsonNumber, JsonObject}
import io.circe.syntax._
import org.msgpack.core.{MessageBufferPacker, MessagePack, MessagePacker => CMessagePacker}
import org.msgpack.core.MessagePack.PackerConfig

import scala.util.control.NonFatal

object MessagePacker {
  def apply(config: PackerConfig = MessagePack.DEFAULT_PACKER_CONFIG): MessagePacker =
    new MessagePacker(config: PackerConfig)
}

final class MessagePacker(config: PackerConfig) {

  private[this] val packer = new ThreadLocal[MessageBufferPacker] {
    override def initialValue(): MessageBufferPacker = config.newBufferPacker()
  }

  def encode[A: Encoder](a: A): Either[Throwable, Array[Byte]] =
    pack(a.asJson)

  def pack(doc: Json): Either[Throwable, Array[Byte]] =
    try {
      val buf = packer.get()
      go(doc, buf)
      Right(buf.toByteArray)
    } catch {
      case NonFatal(t) => Left(t)
    } finally {
      packer.get().clear()
    }

  private def double(x: BigDecimal): Boolean = x.scale != 0

  private def go(json: Json, acc: CMessagePacker): CMessagePacker = {
    json.foldWith(
      new Json.Folder[CMessagePacker] with ((CMessagePacker, (String, Json)) => CMessagePacker) {
        def apply(acc: CMessagePacker, kv: (String, Json)): CMessagePacker = kv match {
          case (k, v) =>
            acc.packString(k)
            v.foldWith(this)
        }
        def onNull: CMessagePacker =
          acc.packNil()
        def onBoolean(value: Boolean): CMessagePacker =
          acc.packBoolean(value)
        def onNumber(value: JsonNumber): CMessagePacker =
          value.toBigDecimal match {
            case None =>
              acc.packNil()
            case Some(v) if double(v) =>
              acc.packDouble(v.toDouble)
            case Some(v) if v.isValidLong =>
              acc.packLong(v.toLong)
            case Some(v) =>
              acc.packBigInteger(v.toBigInt().bigInteger)
          }
        def onString(value: String): CMessagePacker =
          acc.packString(value)
        def onArray(value: Vector[Json]): CMessagePacker =
          value.foldLeft(acc.packArrayHeader(value.size))((_, v) => v.foldWith(this))
        def onObject(value: JsonObject): CMessagePacker =
          value.toIterable.foldLeft(acc.packMapHeader(value.size))(this)
      })
  }
}
