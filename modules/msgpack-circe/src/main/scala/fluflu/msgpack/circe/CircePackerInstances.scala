package fluflu.msgpack.circe

import java.nio.ByteBuffer

import fluflu.msgpack.{Ack, MOption, Packer, Unpacker}
import io.circe.{Decoder, Encoder}
import io.circe.generic.semiauto._
import org.msgpack.core.{MessagePack, MessagePacker => CMessagePacker}

trait CircePackerInstances {
  implicit def circePackerInstance[A: Encoder]: Packer[A] =
    new Packer[A] {
      def apply(a: A, packer: CMessagePacker): Unit =
        MessagePacker(packer).encode(a)
    }

  implicit val encodeMOption: Encoder[MOption] = deriveEncoder[MOption]

  implicit val packMOption: Packer[MOption] = new Packer[MOption] {
    def apply(a: MOption, packer: CMessagePacker): Unit =
      MessagePacker(packer).encode(a)
  }

  private[this] val unpackerConfig = new MessagePack.UnpackerConfig().withBufferSize(124)

  implicit val decodeMOption: Decoder[Ack] = deriveDecoder[Ack]

  implicit val unpackAck: Unpacker[Option[Ack]] = new Unpacker[Option[Ack]] {
    def apply(bytes: ByteBuffer): Either[Throwable, Option[Ack]] =
      MessageUnpacker(unpackerConfig.newUnpacker(bytes)).decode[Option[Ack]]
  }

}
