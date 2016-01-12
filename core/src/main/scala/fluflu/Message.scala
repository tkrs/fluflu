package fluflu

import cats.data.Xor

object Message {

  import msgpack.json.MessagePackJson
  import data.Event
  import io.circe.{ Encoder, Json }
  import io.circe.syntax._

  def pack[A](e: Event[A])(implicit encoder: Encoder[A]): Throwable Xor Array[Byte] = e match {
    case Event(prefix, label, record, time) =>
      val event = Json array (
        Json string s"$prefix.$label",
        Json long time,
        record asJson
      )
      MessagePackJson() pack event
  }
}
