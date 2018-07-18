package fluflu.msgpack.time

import java.time.Instant

import fluflu.msgpack.Packer
import org.msgpack.core.MessagePacker

object eventTime {

  implicit val packInstantAsEventTime: Packer[Instant] = new Packer[Instant] {
    def apply(a: Instant, packer: MessagePacker): Unit = {
      packer.packExtensionTypeHeader(0, 8)
      packer.writePayload(Packer.formatUInt32(a.getEpochSecond))
      packer.writePayload(Packer.formatUInt32(a.getNano.toLong))
    }
  }
}
