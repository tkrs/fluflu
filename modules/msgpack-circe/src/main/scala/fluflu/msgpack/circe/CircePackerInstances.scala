package fluflu.msgpack.circe

import fluflu.msgpack.Packer
import io.circe.Encoder
import org.msgpack.core.{MessagePacker => CMessagePacker}

trait CircePackerInstances {
  implicit def circePackerInstance[A: Encoder]: Packer[A] =
    new Packer[A] {
      def apply(a: A, packer: CMessagePacker): Unit =
        MessagePacker(packer).encode(a)
    }
}
