package fluflu.msgpack.instances

import java.nio.ByteBuffer

import _root_.mess.Fmt
import _root_.mess.codec.Decoder
import _root_.mess.codec.Encoder
import _root_.mess.codec.semiauto.*
import fluflu.msgpack.Ack
import fluflu.msgpack.MOption
import fluflu.msgpack.Packer
import fluflu.msgpack.Unpacker
import org.msgpack.core.MessagePack
import org.msgpack.core.MessagePacker

private[instances] trait MessPackerInstances {
  implicit def packAByMess[A](implicit encodeA: Encoder[A]): Packer[A] =
    new Packer[A] {
      def apply(a: A, packer: MessagePacker): Unit =
        encodeA(a).pack(packer)
    }

  private val encodeMOption: Encoder[MOption] = derivedEncoder[MOption]

  implicit val packMOptionByMess: Packer[MOption] = new Packer[MOption] {
    def apply(a: MOption, packer: MessagePacker): Unit =
      encodeMOption(a).pack(packer)
  }

  private val unpackerConfig = new MessagePack.UnpackerConfig().withBufferSize(124)

  implicit private val decodeOptionAck: Decoder[Option[Ack]] = derivedDecoder[Ack].map(Option.apply)

  implicit val unpackAckByMess: Unpacker[Option[Ack]] = new Unpacker[Option[Ack]] {
    def apply(bytes: ByteBuffer): Either[Throwable, Option[Ack]] =
      decodeOptionAck(Fmt.unpack(unpackerConfig.newUnpacker(bytes)))
  }
}
