package fluflu.msgpack.circe

import java.nio.ByteBuffer

import fluflu.msgpack.States
import fluflu.msgpack.models.{Long10, Long30}
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

  @Benchmark
  def decodeUInt32Circe(data: States.UnpackData): Long = {
    decode[Long](data.uInt32)
  }

  @Benchmark
  def decodeUInt64Circe(data: States.UnpackData): BigInt = {
    decode[BigInt](data.uInt64)
  }

  @Benchmark
  def decodeStr16Circe(data: States.UnpackData): String = {
    decode[String](data.str16V)
  }

  @Benchmark
  def decodeStr32Circe(data: States.UnpackData): String = {
    decode[String](data.str32V)
  }

  @Benchmark
  def decodeLong10Circe(data: States.UnpackData): Long10 = {
    decode[Long10](data.long10CC)
  }

  @Benchmark
  def decodeLong30Circe(data: States.UnpackData): Long30 = {
    decode[Long30](data.long30CC)
  }
}
