package fluflu.msgpack.circe

import java.nio.ByteBuffer

import fluflu.msgpack.States
import fluflu.msgpack.models.{Long10, Long30, Long60}
import io.circe.Decoder
import io.circe.generic.auto._
import org.msgpack.core.MessagePack
import org.openjdk.jmh.annotations._

trait UnpackerBench {

  @inline private def decode[A: Decoder](src: ByteBuffer): A = {
    val p = MessagePack.DEFAULT_UNPACKER_CONFIG.newUnpacker(src)
    val r = MessageUnpacker(p).decode[A].right.get
    p.close()
    r
  }

  private[this] val long10CC: Decoder[Long10] = Decoder[Long10]
  private[this] val long30CC: Decoder[Long30] = Decoder[Long30]
  private[this] val long60CC: Decoder[Long60] = Decoder[Long60]

  @Benchmark
  def decodeUInt32(data: States.UnpackData): Long = {
    decode[Long](data.uInt32)
  }

  @Benchmark
  def decodeUInt64(data: States.UnpackData): BigInt = {
    decode[BigInt](data.uInt64)
  }

  @Benchmark
  def decodeStr16(data: States.UnpackData): String = {
    decode[String](data.str16V)
  }

  @Benchmark
  def decodeStr32(data: States.UnpackData): String = {
    decode[String](data.str32V)
  }

  @Benchmark
  def decodeLong10(data: States.UnpackData): Long10 = {
    decode[Long10](data.long10CC)(long10CC)
  }

  @Benchmark
  def decodeLong30(data: States.UnpackData): Long30 = {
    decode[Long30](data.long30CC)(long30CC)
  }

  @Benchmark
  def decodeLong60(data: States.UnpackData): Long60 = {
    decode[Long60](data.long60CC)(long60CC)
  }
}
