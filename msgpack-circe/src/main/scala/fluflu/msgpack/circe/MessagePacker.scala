package fluflu.msgpack
package circe

import cats.syntax.either._
import io.circe.{Encoder, Json}
import io.circe.syntax._
import org.msgpack.core.MessagePack.PackerConfig
import org.msgpack.core.{MessageBufferPacker, MessagePack}

object MessagePacker {
  def apply(config: PackerConfig = MessagePack.DEFAULT_PACKER_CONFIG): MessagePacker =
    new MessagePacker(config: PackerConfig)
}

final class MessagePacker(config: PackerConfig) {

  def encode[A: Encoder](a: A): Either[Throwable, Array[Byte]] = pack(a.asJson)

  def pack(doc: Json): Either[Throwable, Array[Byte]] = Either.catchNonFatal {
    val msgPack = config.newBufferPacker()
    go(doc, msgPack)
    msgPack.toByteArray
  }

  def double(x: BigDecimal): Boolean = x.scale != 0

  def go(json: Json, acc: MessageBufferPacker): Unit =
    json.fold[Unit](
      acc.packNil(),
      acc.packBoolean,
      x => {
        val n = x.toBigDecimal
        n match {
          case None => ()
          case Some(v) if double(v) =>
            acc.packDouble(v.toDouble)
          case Some(v) if v.isValidLong =>
            acc.packLong(v.toLong)
          case Some(v) =>
            acc.packBigInteger(v.toBigInt().bigInteger)
        }
      },
      acc.packString,
      xs => {
        acc.packArrayHeader(xs.size)
        xs.foreach(go(_, acc))
      },
      x => {
        val xs = x.toList
        acc.packMapHeader(xs.size)
        xs.foreach {
          case (key, (v)) =>
            acc.packString(key)
            go(v, acc)
        }
      }
    )
}
