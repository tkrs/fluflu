package fluflu

import java.nio.ByteBuffer

import cats.data.Xor
import data.Event
import fluflu.msgpack.{ JSON, MessagePack }
import io.circe.{ Encoder, Json }
import io.circe.syntax._

object Message {
  private[this] val packer = MessagePack.getInstance(JSON)

  def pack[A](e: Event[A])(implicit A: Encoder[A]): Throwable Xor ByteBuffer = e match {
    case Event(prefix, label, record, time) =>
      val event = Json arr (
        Json fromString s"$prefix.$label",
        Json fromLong time,
        record asJson
      )
      packer pack event map { arr =>
        val b = ByteBuffer.allocateDirect(arr.length)
        b.put(arr, 0, arr.length).flip()
        b
      }
  }
}
