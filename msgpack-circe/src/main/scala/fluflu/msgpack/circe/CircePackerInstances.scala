package fluflu.msgpack.circe

import fluflu.msgpack.Packer
import io.circe.Encoder
import org.msgpack.core.MessagePack

trait CircePackerInstances {
  implicit def circePackerInstance[A: Encoder]: Packer[A] =
    new Packer[A] {
      private[this] val circeMsgPacker = MessagePacker(MessagePack.DEFAULT_PACKER_CONFIG)

      override def apply(a: A): Either[Throwable, Array[Byte]] = circeMsgPacker.encode(a)
    }
}
