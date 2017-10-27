package fluflu.msgpack.circe

import fluflu.msgpack.Packer
import io.circe.Encoder

trait CircePackerInstances {
  private[this] val circeMsgPacker = new MessagePacker
  implicit def circePackerInstance[A: Encoder]: Packer[A] = new Packer[A] {
    override def apply(a: A): Either[Throwable, Array[Byte]] = circeMsgPacker.encode(a)
  }
}
