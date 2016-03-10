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

    val go: (Json) => Vector[Byte] = _.fold(
      {
        nilFormat()
      },
      { x: Boolean =>
        boolFormat(x)
      },
      { x: JsonNumber =>
        val n = x.toBigDecimal
        n match {
          case None => throw new ArithmeticException()
          case Some(v) =>
            if (v.isWhole() && Long.MinValue <= v)
              intFormat(v.toLong)
            else
              formatOfDouble(v.toDouble)
        }
      },
      { xs: String =>
        formatOfString(xs)
      },
      { xs: List[Json] =>
        markArray(xs.size) ++ xs.foldLeft(Vector.empty[Byte])(_ ++ go(_))
      },
      { x: JsonObject =>
        val xs = x.toList
        val vec = markMap(xs.size)
        vec ++ xs.foldLeft(Vector.empty[Byte]) {
          case (acc, (key, v)) =>
            acc ++ formatOfString(key) ++ go(v)
        }
      }
    )

    override def unpack(a: Array[Byte]): Option[Json] = ???
  }
}
