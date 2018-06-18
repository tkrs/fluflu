package fluflu.msgpack.shapes

import java.nio.ByteBuffer

import fluflu.msgpack.{States, models}
import org.msgpack.core.MessagePack
import org.openjdk.jmh.annotations._
import derivedCodecs._

trait UnpackerBench {

  @inline private def decode[A: Decoder](src: ByteBuffer): A = {
    val p   = MessagePack.DEFAULT_UNPACKER_CONFIG.newUnpacker(src)
    val dst = Codec.deserialize(p)
    val r   = Decoder[A].apply(dst).right.get
    p.close()
    r
  }

  @Benchmark
  def decodeUInt32Shapes(data: States.UnpackData): Long = {
    decode[Long](data.uInt32)
  }

  @Benchmark
  def decodeUInt64Shapes(data: States.UnpackData): BigInt = {
    decode[BigInt](data.uInt64)
  }

  @Benchmark
  def decodeStr16Shapes(data: States.UnpackData): String = {
    decode[String](data.str16V)
  }

  @Benchmark
  def decodeStr32Shapes(data: States.UnpackData): String = {
    decode[String](data.str32V)
  }

  @Benchmark
  def decodeLong10Shapes(data: States.UnpackData): models.Long10 = {
    decode[models.Long10](data.long10CC)
  }

  @Benchmark
  def decodeLong30Shapes(data: States.UnpackData): models.Long30 = {
    decode[models.Long30](data.long30CC)
  }

  @Benchmark
  def decodeLong60Shapes(data: States.UnpackData): models.Long60 = {
    decode[models.Long60](data.long60CC)
  }

  @Benchmark
  def decodeLong10ShapesCache(data: States.UnpackData): models.Long10 = {
    decode[models.Long10](data.long10CC)(Decoders.Long10CC)
  }

  @Benchmark
  def decodeLong30ShapesCache(data: States.UnpackData): models.Long30 = {
    decode[models.Long30](data.long30CC)(Decoders.Long30CC)
  }

  @Benchmark
  def decodeLong60ShapesCache(data: States.UnpackData): models.Long60 = {
    decode[models.Long60](data.long60CC)(Decoders.Long60CC)
  }
}

object Decoders {
  final val Long10CC: Decoder[models.Long10] = Decoder[models.Long10]
  final val Long30CC: Decoder[models.Long30] = Decoder[models.Long30]
  final val Long60CC: Decoder[models.Long60] = Decoder[models.Long60]
}
