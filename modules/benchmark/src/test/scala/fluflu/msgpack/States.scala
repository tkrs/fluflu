package fluflu.msgpack

import java.nio.ByteBuffer

import fluflu.msgpack.circe.{MessagePacker, MessageUnpacker}
import fluflu.msgpack.models._
import org.msgpack.core.{MessageBufferPacker, MessagePack}
import org.openjdk.jmh.annotations.{Scope, Setup, State, TearDown}

object States {

  def newPacker = MessagePack.DEFAULT_PACKER_CONFIG.newBufferPacker()
  def packer    = MessagePacker(newPacker)
  def unpacker(bytes: Array[Byte]) =
    MessageUnpacker(MessagePack.DEFAULT_UNPACKER_CONFIG.newUnpacker(bytes))

  @State(Scope.Benchmark)
  class PackData {
    final val Str16    = StringT.str16
    final val Str32    = StringT.str32
    final val UInt32   = IntT.uInt32
    final val UInt64   = IntT.uInt64
    final val Long10CC = Long10.default
    final val Long30CC = Long30.default

    var packer: MessageBufferPacker = _

    @Setup
    def open(): Unit = {
      packer = newPacker
    }

    @TearDown
    def close(): Unit = {
      packer.close()
    }
  }

  @State(Scope.Benchmark)
  class UnpackData {
    private[this] val uInt32_            = IntT.uInt32Bytes
    @inline final def uInt32: ByteBuffer = uInt32_.duplicate()

    private[this] val uInt64_            = IntT.uInt64Bytes
    @inline final def uInt64: ByteBuffer = uInt64_.duplicate()

    private[this] val str16V_            = StringT.str16Bytes
    @inline final def str16V: ByteBuffer = str16V_.duplicate()

    private[this] val str32V_            = StringT.str32Bytes
    @inline final def str32V: ByteBuffer = str32V_.duplicate()

    private[this] val long10CC_            = Long10.bytes
    @inline final def long10CC: ByteBuffer = long10CC_.duplicate()

    private[this] val long30CC_            = Long30.bytes
    @inline final def long30CC: ByteBuffer = long30CC_.duplicate()
  }
}
