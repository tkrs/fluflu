package fluflu

import java.nio.ByteBuffer

import io.circe.{ Encoder, Json }
import io.circe.syntax._

import scala.util.{ Either => \/ }

object Messages {

  private[this] val packer = msgpack.MessagePacker()

  def pack[A](e: Event[A])(implicit A: Encoder[A]): Throwable \/ Array[Byte] = e match {
    case Event(prefix, label, record, time) =>
      packer pack (Json arr (
        Json fromString s"$prefix.$label",
        Json fromLong time,
        record.asJson
      ))
  }

  private[this] val buffers = new ThreadLocal[ByteBuffer] {
    override def initialValue(): ByteBuffer = ByteBuffer.allocateDirect(1024)
  }

  def getBuffer(len: Int): ByteBuffer = {
    val buffer = buffers.get()
    if (buffer.limit >= len) buffer else {
      if (buffer.capacity() < len) {
        buffer.clear()
        val newBuffer = ByteBuffer.allocateDirect(len)
        buffers.set(newBuffer)
        newBuffer
      } else {
        buffer.limit(len)
        buffer
      }
    }
  }
}
