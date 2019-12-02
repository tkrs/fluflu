package fluflu.msgpack.mess

import java.nio.ByteBuffer

import fluflu.msgpack.{Ack, MOption, Packer, Unpacker}
import mess.Fmt
import mess.codec.{Decoder, Encoder}
import org.msgpack.core.{MessagePack, MessagePacker => CMessagePacker}

trait MessPackerInstances {
  import mess.codec.auto._

  implicit def packAByMess[A](implicit encodeA: Encoder[A]): Packer[A] =
    new Packer[A] {

      def apply(a: A, packer: CMessagePacker): Unit =
        encodeA(a).pack(packer)
    }

  private[this] val encodeMOption: Encoder[MOption] = Encoder[MOption]

  implicit val packMOptionByMess: Packer[MOption] = new Packer[MOption] {

    def apply(a: MOption, packer: CMessagePacker): Unit =
      encodeMOption(a).pack(packer)
  }

  private[this] val unpackerConfig = new MessagePack.UnpackerConfig().withBufferSize(124)

  implicit private[this] val decodeOptionAck: Decoder[Option[Ack]] = Decoder[Ack].map(Option.apply)

  implicit val unpackAckByMess: Unpacker[Option[Ack]] = new Unpacker[Option[Ack]] {

    def apply(bytes: ByteBuffer): Either[Throwable, Option[Ack]] =
      decodeOptionAck(Fmt.unpack(unpackerConfig.newUnpacker(bytes)))
  }
}
