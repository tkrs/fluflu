package fluflu.msgpack.shapes

import fluflu.msgpack.Packer
import org.msgpack.core.MessagePacker

trait ShapelessPackerInstances {
  implicit def shapelessPackerInstance[A: Encoder]: Packer[A] =
    new Packer[A] {
      def apply(a: A, packer: MessagePacker): Unit =
        Codec.serialize(Encoder[A].apply(a), packer)
    }
}
