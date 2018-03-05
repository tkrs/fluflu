package fluflu.time

import java.time.Instant

import fluflu.msgpack.Packer

import scala.collection.mutable

object integer {
  implicit val packInstantAsInteger: Packer[Instant] = new Packer[Instant] {
    def apply(a: Instant): Either[Throwable, Array[Byte]] = {
      val acc = mutable.ArrayBuilder.make[Byte]
      acc += 0xce.toByte
      Packer.formatUInt32(a.getEpochSecond, acc)
      Right(acc.result())
    }
  }
}
