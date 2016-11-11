package fluflu

import fluflu.msgpack.{ JSON, MessagePack }
import io.circe.{ Encoder, Json }
import io.circe.syntax._

import scala.util.{ Either => \/ }

object Message {
  private[this] val packer = MessagePack.getInstance(JSON)

  def pack[A](e: Event[A])(implicit A: Encoder[A]): Throwable \/ Array[Byte] = e match {
    case Event(prefix, label, record, time) =>
      packer pack (Json arr (
        Json fromString s"$prefix.$label",
        Json fromLong time,
        record.asJson
      ))
  }
}
