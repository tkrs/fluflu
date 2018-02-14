package fluflu.msgpack
package circe

import io.circe.{Encoder, Json, JsonNumber, JsonObject}
import io.circe.syntax._
import org.msgpack.core.{MessagePacker => CMessagePacker}
import org.msgpack.core.MessagePack
import org.msgpack.core.MessagePack.PackerConfig

import scala.util.{Failure, Success, Try}

object MessagePacker {
  def apply(config: PackerConfig = MessagePack.DEFAULT_PACKER_CONFIG): MessagePacker =
    new MessagePacker(config: PackerConfig)
}

final class MessagePacker(config: PackerConfig) {

  def encode[A: Encoder](a: A): Either[Throwable, Array[Byte]] =
    pack(a.asJson)

  def pack(doc: Json): Either[Throwable, Array[Byte]] =
    Packer.using(Try(config.newBufferPacker()))(r => Try { val _ = go(doc, r); r.toByteArray }) match {
      case Success(v) => Right(v)
      case Failure(e) => Left(e)
    }

  private def double(x: BigDecimal): Boolean = x.scale != 0

  private def go(json: Json, acc: CMessagePacker): CMessagePacker = {
    json.foldWith(
      new Json.Folder[CMessagePacker] with ((CMessagePacker, (String, Json)) => CMessagePacker) {
        def apply(acc: CMessagePacker, kv: (String, Json)): CMessagePacker = kv match {
          case (k, v) =>
            acc.packString(k)
            v.foldWith(this)
            acc
        }
        def onNull: CMessagePacker =
          acc.packNil()
        def onBoolean(value: Boolean): CMessagePacker =
          acc.packBoolean(value)
        def onNumber(value: JsonNumber): CMessagePacker =
          value.toBigDecimal match {
            case None =>
              acc
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
        def onObject(value: JsonObject): CMessagePacker = {
          val xs = value.toVector
          xs.foldLeft(acc.packMapHeader(xs.size))(this)
        }
      })
  }
}
