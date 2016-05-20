package fluflu.msgpack.json

import cats.data.Xor
import io.circe.{ JsonObject, JsonNumber, HCursor, Json }
import fluflu.msgpack.MessagePack
import scala.collection.mutable.ListBuffer

object MessagePackJson {

  import MessagePack._

  def apply() = new MessagePack[Json]() {

    override def pack(doc: Json): Throwable Xor Array[Byte] =
      Xor.catchOnly[Throwable](go(doc).toArray)

    def double(x: BigDecimal): Boolean =
      (x.isDecimalDouble || x.isBinaryDouble || x.isExactDouble) && x.scale > 0

    val go: (Json) => Vector[Byte] = _.fold(
      {
        formatNil
      },
      { x: Boolean =>
        formatBoolFamily(x)
      },
      { x: JsonNumber =>
        val n = x.toBigDecimal
        n match {
          case None =>
            throw new ArithmeticException()
          case Some(v) if double(v) =>
            formatFloatFamily(v.toDouble)
          case Some(v) if v.isValidLong =>
            formatIntFamily(v.toLong)
          case Some(v) =>
            formatIntFamily(BigInt(0xcf.toLong), v.toBigInt())
        }
      },
      { xs: String =>
        formatStrFamily(xs)
      },
      { xs: List[Json] =>
        formatArrayFamilyHeader(xs.size) ++ xs.foldLeft(Vector.empty[Byte])(_ ++ go(_))
      },
      { x: JsonObject =>
        val xs = x.toList
        val vec = formatMapFamilyHeader(xs.size)
        vec ++ xs.foldLeft(Vector.empty[Byte]) {
          case (acc, (key, v)) =>
            acc ++ formatStrFamily(key) ++ go(v)
        }
      }
    )

    override def unpack(a: Array[Byte]): Option[Json] = ???
  }
}
