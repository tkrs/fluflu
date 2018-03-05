package fluflu.time

import java.time.Instant

import fluflu.msgpack.Packer

import scala.collection.mutable

object eventTime {

  implicit val packInstantAsEventTime: Packer[Instant] = new Packer[Instant] {
    def apply(a: Instant): Either[Throwable, Array[Byte]] = {
      val acc = mutable.ArrayBuilder.make[Byte]
      acc += 0xd7.toByte
      acc += 0x00.toByte
      Packer.formatUInt32(a.getEpochSecond, acc)
      Packer.formatUInt32(a.getNano.toLong, acc)
      Right(acc.result())
    }
  }
}
