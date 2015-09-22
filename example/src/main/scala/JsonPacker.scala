import fluflu.msgpack._
import io.circe.Json

object JsonPacker extends App {
  def bytesToLong(buf: Seq[Byte]): Long =
    buf.foldRight((0, 0L))((l, r) => r match {
      case (i, n) => (i + 8, ((l & 0xff).toLong << i) | n)
    })._2

  val instance = MessagePack getInstance JSON

  val x = instance pack Json.long(Long.MaxValue)

  println(bytesToLong(x) == Long.MaxValue)

}
