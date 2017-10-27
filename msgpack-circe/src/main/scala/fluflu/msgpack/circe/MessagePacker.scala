package fluflu.msgpack
package circe

import cats.syntax.either._
import io.circe.{Encoder, Json}
import io.circe.syntax._

import scala.collection.mutable

object MessagePacker {
  def apply(): MessagePacker = new MessagePacker
}

final class MessagePacker {
  import Packer._

  def encode[A: Encoder](a: A): Either[Throwable, Array[Byte]] = pack(a.asJson)

  def pack(doc: Json): Either[Throwable, Array[Byte]] = Either.catchNonFatal {
    val acc = mutable.ArrayBuilder.make[Byte]
    go(doc, acc)
    acc.result
  }

  def double(x: BigDecimal): Boolean = x.scale != 0

  def go(json: Json, acc: mutable.ArrayBuilder[Byte]): Unit =
    json.fold[Unit](
      formatNil(acc),
      x => formatBoolFamily(x, acc),
      x => {
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
      },
      x => formatStrFamily(x, acc),
      xs => {
        formatArrayFamilyHeader(xs.size, acc)
        xs.foreach(go(_, acc))
      },
      x => {
        val xs = x.toList
        formatMapFamilyHeader(xs.size, acc)
        xs.foreach {
          case (key, (v)) =>
            formatStrFamily(key, acc)
            go(v, acc)
        }
      }
    )
}
