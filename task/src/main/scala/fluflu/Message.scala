package fluflu

import cats.data.Xor

object Message {

  import msgpack.json.MessagePackJson
  import data.Event
  import io.circe.{ Encoder, Json }
  import io.circe.syntax._

  final def pack[A](e: Event[A])(implicit encoder: Encoder[A]): Throwable Xor Array[Byte] = e match {
    case Event(prefix, label, record, time) =>
      val event = Json arr (
        Json fromString s"$prefix.$label",
        Json fromLong time,
        record asJson
      )
      MessagePackJson() pack event
  }
}
