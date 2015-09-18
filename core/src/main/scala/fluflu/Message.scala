package fluflu

object Message {

  import msgpack.json.MessagePackJson
  import data.Event
  import io.circe.{ Encoder, Json }
  import io.circe.syntax._

  def pack[A](e: Event[A])(implicit encoder: Encoder[A]): Array[Byte] = e match {
    case Event(prefix, label, record, time) =>
      val event = Json array (
        Json string s"$prefix.$label",
        Json long time,
        record asJson
      )
      val pack = MessagePackJson() pack event
      pack toArray
  }
}
