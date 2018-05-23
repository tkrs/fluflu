package fluflu.msgpack.circe

import fluflu.msgpack.States
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

  @Benchmark
  def encodeUInt32Circe(data: States.PackData): Array[Byte] = {
    encode(data.UInt32, data.packer)
  }

  @Benchmark
  def encodeUInt64Circe(data: States.PackData): Array[Byte] = {
    encode(data.UInt64, data.packer)
  }

  @Benchmark
  def encodeStr16Circe(data: States.PackData): Array[Byte] = {
    encode(data.Str16, data.packer)
  }

  @Benchmark
  def encodeStr32Circe(data: States.PackData): Array[Byte] = {
    encode(data.Str32, data.packer)
  }

  @Benchmark
  def encodeLong10Circe(data: States.PackData): Array[Byte] = {
    encode(data.Long10CC, data.packer)
  }

  @Benchmark
  def encodeLong30Circe(data: States.PackData): Array[Byte] = {
    encode(data.Long30CC, data.packer)
  }
}
