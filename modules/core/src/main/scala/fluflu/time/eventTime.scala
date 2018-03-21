package fluflu.time

import java.time.Instant

import fluflu.msgpack.Packer
import org.msgpack.core.MessagePack.Code

object eventTime {

  implicit val packInstantAsEventTime: Packer[Instant] = new Packer[Instant] {
    val NULL: Byte = 0x00.toByte
    def apply(a: Instant): Either[Throwable, Array[Byte]] = {
      val seconds = Packer.formatUInt32(a.getEpochSecond)
      val nanos   = Packer.formatUInt32(a.getNano.toLong)
      val dest    = Array.ofDim[Byte](2 + seconds.length + nanos.length)
      dest(0) = Code.FIXEXT8
      dest(1) = NULL
      java.lang.System.arraycopy(seconds, 0, dest, 2, seconds.length)
      java.lang.System.arraycopy(nanos, 0, dest, 2 + seconds.length, nanos.length)
      Right(dest)
    }
  }
}
