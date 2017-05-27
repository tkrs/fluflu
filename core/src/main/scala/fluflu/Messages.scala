package fluflu

import java.nio.ByteBuffer

import io.circe.Encoder

object Messages {

  def pack[A: Encoder](e: Event[A]): Either[Throwable, Array[Byte]] = e.pack

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
