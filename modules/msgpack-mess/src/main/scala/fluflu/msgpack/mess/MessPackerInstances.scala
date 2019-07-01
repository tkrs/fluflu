package fluflu.msgpack.mess

import java.nio.ByteBuffer

import fluflu.msgpack.{Ack, MOption, Packer, Unpacker}
import mess.{Decoder, Encoder}
import mess.ast.MsgPack
import mess.codec.generic._
import org.msgpack.core.{MessagePack, MessagePacker => CMessagePacker}

trait MessPackerInstances {

  implicit def packAByMess[A](implicit encodeA: Encoder[A]): Packer[A] =
    new Packer[A] {
      def apply(a: A, packer: CMessagePacker): Unit =
        encodeA(a).pack(packer)
    }

  private[this] val encodeMOption: Encoder[MOption] = derivedEncoder[MOption]

  implicit val packMOptionByMess: Packer[MOption] = new Packer[MOption] {
    def apply(a: MOption, packer: CMessagePacker): Unit =
      encodeMOption(a).pack(packer)
  }

  private[this] val unpackerConfig = new MessagePack.UnpackerConfig().withBufferSize(124)

  implicit private[this] val decodeMOption: Decoder[Ack] = derivedDecoder[Ack]

  implicit val unpackAckByMess: Unpacker[Option[Ack]] = new Unpacker[Option[Ack]] {
    def apply(bytes: ByteBuffer): Either[Throwable, Option[Ack]] =
      Decoder[Option[Ack]].apply(MsgPack.unpack(unpackerConfig.newUnpacker(bytes)))
  }
}
