package fluflu

import java.nio.ByteBuffer

import cats.syntax.either._
import fluflu.msgpack.MessagePacker
import io.circe.{ Encoder, Json }
import io.circe.syntax._

import scala.util.{ Either => \/ }

trait Message[+T] {
  def apply[A: Encoder](evt: Event[A]): Throwable \/ T
}

object Message {

  def apply[A: Encoder](implicit A: Message[A]): Message[A] = A

  implicit val jsonMessage: Message[Json] = new Message[Json] {
    def apply[A: Encoder](evt: Event[A]): Throwable \/ Json = {
      val j = Json arr (
        Json fromString s"${evt.prefix}.${evt.label}",
        Json fromLong evt.time,
        evt.record.asJson
      )
      \/.right(j)
    }
  }

  implicit val byteBufferMessage: Message[ByteBuffer] = new Message[ByteBuffer] {
    private[this] val packer: MessagePacker = MessagePacker()
    def apply[A: Encoder](evt: Event[A]): Throwable \/ ByteBuffer = {
      val j = Json arr (
        Json fromString s"${evt.prefix}.${evt.label}",
        Json fromLong evt.time,
        evt.record.asJson
      )
      packer.pack(j).map(ByteBuffer.wrap)
    }
  }

}
