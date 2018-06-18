package fluflu.msgpack.shapes

import fluflu.msgpack.{States, models}
import fluflu.msgpack.shapes.ast.MsgPack
import org.msgpack.core.MessageBufferPacker
import org.openjdk.jmh.annotations._

import derivedCodecs._

trait PackerBench {

  @inline private def encode[A](a: A, p: MessageBufferPacker)(
      implicit A: Encoder[A]): Array[Byte] = {
    Codec.serialize(A(a), p)
    val r = p.toByteArray
    p.clear()
    r
  }

  @Benchmark
  def encodeUInt32Shapes(data: States.PackData): Array[Byte] = {
    encode(data.UInt32, data.packer)
  }

  @Benchmark
  def encodeUInt64Shapes(data: States.PackData): Array[Byte] = {
    encode(data.UInt64, data.packer)
  }

  @Benchmark
  def encodeStr16Shapes(data: States.PackData): Array[Byte] = {
    encode(data.Str16, data.packer)
  }

  @Benchmark
  def encodeStr32Shapes(data: States.PackData): Array[Byte] = {
    encode(data.Str32, data.packer)
  }

  @Benchmark
  def encodeLong10Shapes(data: States.PackData): Array[Byte] = {
    encode(data.Long10CC, data.packer)
  }

  @Benchmark
  def encodeLong30Shapes(data: States.PackData): Array[Byte] = {
    encode(data.Long30CC, data.packer)
  }

  @Benchmark
  def encodeLong60Shapes(data: States.PackData): Array[Byte] = {
    encode(data.Long60CC, data.packer)
  }
}

trait PackAstBench {

  @inline private def encode[A](a: A)(implicit A: Encoder[A]): MsgPack = A(a)

  @Benchmark
  def encodeLong10Shapes(data: States.PackData): MsgPack = {
    encode(data.Long10CC)
  }

  @Benchmark
  def encodeLong30Shapes(data: States.PackData): MsgPack = {
    encode(data.Long30CC)
  }

  @Benchmark
  def encodeLong60Shapes(data: States.PackData): MsgPack = {
    encode(data.Long60CC)
  }

  @Benchmark
  def encodeLong10ShapesCache(data: States.PackData): MsgPack = {
    encode(data.Long10CC)(Encoders.Long10CC)
  }

  @Benchmark
  def encodeLong30ShapesCache(data: States.PackData): MsgPack = {
    encode(data.Long30CC)(Encoders.Long30CC)
  }

  @Benchmark
  def encodeLong60ShapesCache(data: States.PackData): MsgPack = {
    encode(data.Long60CC)(Encoders.Long60CC)
  }
}

object Encoders {
  final val Long10CC: Encoder[models.Long10] = Encoder[models.Long10]
  final val Long30CC: Encoder[models.Long30] = Encoder[models.Long30]
  final val Long60CC: Encoder[models.Long60] = Encoder[models.Long60]
}
