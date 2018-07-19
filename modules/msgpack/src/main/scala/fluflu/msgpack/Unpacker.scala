package fluflu.msgpack

import java.nio.ByteBuffer

trait Unpacker[A] {
  def apply(bytes: ByteBuffer): Either[Throwable, A]
}
