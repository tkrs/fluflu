package fluflu.time

import java.time.Instant

import fluflu.msgpack.Packer
import org.msgpack.core.MessagePack.Code

object integer {
  implicit val packInstantAsInteger: Packer[Instant] = new Packer[Instant] {
    def apply(a: Instant): Either[Throwable, Array[Byte]] = {
      Right(Code.UINT32 +: Packer.formatUInt32(a.getEpochSecond))
    }
  }
}
