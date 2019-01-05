package fluflu.msgpack.time

import java.time.Instant

import fluflu.msgpack.Packer
import org.msgpack.core.MessagePack.Code
import org.msgpack.core.MessagePacker

object integer {
  implicit val packInstantAsInteger: Packer[Instant] = new Packer[Instant] {
    def apply(a: Instant, packer: MessagePacker): Unit =
      packer.addPayload(Code.UINT32 +: Packer.formatUInt32(a.getEpochSecond))
  }
}
