package fluflu.msgpack.mess

import java.nio.ByteBuffer

import fluflu.msgpack.{Ack, MOption, Packer, Unpacker}
import mess.codec.Codec
import mess.{Decoder, Encoder}
import mess.codec.generic._
import org.msgpack.core.{MessagePack, MessagePacker => CMessagePacker}

trait MessPackerInstances {

  implicit def packAByMess[A](implicit encodeA: Encoder[A]): Packer[A] =
    new Packer[A] {
      def apply(a: A, packer: CMessagePacker): Unit = {
        Codec.serialize(encodeA(a), packer)
      }
    }

  private[this] val encodeMOption: Encoder[MOption] = derivedEncoder[MOption]

  implicit val packMOptionByMess: Packer[MOption] = new Packer[MOption] {
    def apply(a: MOption, packer: CMessagePacker): Unit =
      Codec.serialize(encodeMOption(a), packer)
  }

  private[this] val unpackerConfig = new MessagePack.UnpackerConfig().withBufferSize(124)

  private[this] implicit val decodeMOption: Decoder[Ack] = derivedDecoder[Ack]

  implicit val unpackAckByMess: Unpacker[Option[Ack]] = new Unpacker[Option[Ack]] {
    def apply(bytes: ByteBuffer): Either[Throwable, Option[Ack]] =
      Decoder[Option[Ack]].apply(Codec.deserialize(unpackerConfig.newUnpacker(bytes)))
  }
}
