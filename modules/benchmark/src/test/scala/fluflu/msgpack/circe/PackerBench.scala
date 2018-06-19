package fluflu.msgpack.circe

import fluflu.msgpack.{States, models}
import io.circe.Encoder
import io.circe.generic.auto._
import org.msgpack.core.MessageBufferPacker
import org.openjdk.jmh.annotations._

trait PackerBench {

  @inline private def encode[A: Encoder](a: A, p: MessageBufferPacker): Array[Byte] = {
    MessagePacker(p).encode(a)
    val r = p.toByteArray
    p.clear()
    r
  }

  private[this] val long10CC: Encoder[models.Long10] = Encoder[models.Long10]
  private[this] val long30CC: Encoder[models.Long30] = Encoder[models.Long30]
  private[this] val long60CC: Encoder[models.Long60] = Encoder[models.Long60]

  @Benchmark
  def encodeUInt32(data: States.PackData): Array[Byte] = {
    encode(data.UInt32, data.packer)
  }

  @Benchmark
  def encodeUInt64(data: States.PackData): Array[Byte] = {
    encode(data.UInt64, data.packer)
  }

  @Benchmark
  def encodeStr16(data: States.PackData): Array[Byte] = {
    encode(data.Str16, data.packer)
  }

  @Benchmark
  def encodeStr32(data: States.PackData): Array[Byte] = {
    encode(data.Str32, data.packer)
  }

  @Benchmark
  def encodeLong10(data: States.PackData): Array[Byte] = {
    encode(data.Long10CC, data.packer)(long10CC)
  }

  @Benchmark
  def encodeLong30(data: States.PackData): Array[Byte] = {
    encode(data.Long30CC, data.packer)(long30CC)
  }

  @Benchmark
  def encodeLong60(data: States.PackData): Array[Byte] = {
    encode(data.Long60CC, data.packer)(long60CC)
  }
}
