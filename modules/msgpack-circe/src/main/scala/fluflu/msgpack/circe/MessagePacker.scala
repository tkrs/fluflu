package fluflu.msgpack
package circe

import io.circe.{Encoder, Json, JsonNumber, JsonObject}
import io.circe.syntax._
import org.msgpack.core.{MessagePacker => MPacker}

object MessagePacker {
  def apply(packer: MPacker): MessagePacker =
    new MessagePacker(packer)
}

final class MessagePacker(packer: MPacker) {

  def encode[A: Encoder](a: A): Unit =
    pack(a.asJson)

  def pack(doc: Json): Unit =
    go(doc, packer)

  private def double(x: BigDecimal): Boolean = x.scale != 0

  private def go(json: Json, acc: MPacker): MPacker = {
    json.foldWith(new Json.Folder[MPacker] with ((MPacker, (String, Json)) => MPacker) {
      def apply(acc: MPacker, kv: (String, Json)): MPacker = kv match {
        case (k, v) =>
          acc.packString(k)
          v.foldWith(this)
      }
      def onNull: MPacker =
        acc.packNil()
      def onBoolean(value: Boolean): MPacker =
        acc.packBoolean(value)
      def onNumber(value: JsonNumber): MPacker =
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
      def onString(value: String): MPacker =
        acc.packString(value)
      def onArray(value: Vector[Json]): MPacker =
        value.foldLeft(acc.packArrayHeader(value.size))((_, v) => v.foldWith(this))
      def onObject(value: JsonObject): MPacker =
        value.toIterable.foldLeft(acc.packMapHeader(value.size))(this)
    })
  }
}
