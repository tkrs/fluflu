package fluflu.msgpack.circe

import java.nio.ByteBuffer

import cats.syntax.either._
import fluflu.msgpack.Packer
import io.circe.{Decoder, DecodingFailure, Error, Json}
import org.msgpack.core.{MessagePack, MessageFormat => MF}
import org.msgpack.core.MessagePack.UnpackerConfig
import org.msgpack.core.{MessageUnpacker => MUnpacker}

import scala.annotation.tailrec
import scala.collection.mutable
import scala.util.Try

object MessageUnpacker {

  def apply(byteBuffer: ByteBuffer,
            config: UnpackerConfig = MessagePack.DEFAULT_UNPACKER_CONFIG): MessageUnpacker =
    new MessageUnpacker(byteBuffer, config)

}

final class MessageUnpacker(src: ByteBuffer, config: UnpackerConfig) {

  def decode[A: Decoder]: Either[Error, A] =
    Either
      .catchOnly[Exception](unpack)
      .leftMap(e => DecodingFailure(e.getMessage, List.empty))
      .flatMap(_.as[A])

  def unpack: Json = {
    Packer
      .using(Try(config.newUnpacker(src)))(r => Try(unpack0(r)))
      .get
  }

  private def unpack0(buffer: MUnpacker): Json =
    if (!buffer.hasNext) Json.obj()
    else
      buffer.getNextFormat() match {
        case MF.NIL =>
          buffer.skipValue()
          Json.Null
        case MF.BOOLEAN =>
          Json.fromBoolean(buffer.unpackBoolean())
        case MF.FLOAT32 =>
          Json.fromFloatOrNull(buffer.unpackFloat())
        case MF.FLOAT64 =>
          Json.fromDoubleOrNull(buffer.unpackDouble())
        case MF.POSFIXINT | MF.NEGFIXINT | MF.INT8 | MF.INT16 | MF.INT32 | MF.UINT8 | MF.UINT16 =>
          Json.fromInt(buffer.unpackInt())
        case MF.UINT32 | MF.INT64 =>
          Json.fromLong(buffer.unpackLong())
        case MF.UINT64 =>
          Json.fromBigInt(buffer.unpackBigInteger())
        case MF.BIN8 | MF.BIN16 | MF.BIN32 | MF.STR8 | MF.STR16 | MF.STR32 | MF.FIXSTR =>
          Json.fromString(buffer.unpackString())
        case MF.FIXARRAY | MF.ARRAY16 | MF.ARRAY32 =>
          val size = buffer.unpackArrayHeader()
          unpackList(size, buffer)
        case MF.FIXMAP | MF.MAP16 | MF.MAP32 =>
          val size = buffer.unpackMapHeader()
          unpackMap(size, buffer)
        case MF.EXT8 =>
          // TODO:
          throw new Exception("Unsupported type: EXT8")
        case MF.EXT16 =>
          // TODO:
          throw new Exception("Unsupported type: EXT16")
        case MF.EXT32 =>
          // TODO:
          throw new Exception("Unsupported type: EXT32")
        case MF.FIXEXT1 =>
          // TODO:
          throw new Exception("Unsupported type: FIXEXT1")
        case MF.FIXEXT2 =>
          // TODO:
          throw new Exception("Unsupported type: FIXEXT2")
        case MF.FIXEXT4 =>
          // TODO:
          throw new Exception("Unsupported type: FIXEXT4")
        case MF.FIXEXT8 =>
          // TODO:
          throw new Exception("Unsupported type: FIXEXT8")
        case MF.FIXEXT16 =>
          // TODO:
          throw new Exception("Unsupported type: FIXEXT16")
        case MF.NEVER_USED =>
          Json.Null
      }

  private def unpackList(limit: Int, buffer: MUnpacker): Json = {
    @tailrec def loop(i: Int, acc: mutable.Builder[Json, Vector[Json]]): Vector[Json] =
      if (i == limit) acc.result() else loop(i + 1, acc += unpack0(buffer))
    Json.fromValues(loop(0, Vector.newBuilder[Json]))
  }

  private def unpackMap(size: Int, buffer: MUnpacker): Json = {
    @tailrec def loop(
        i: Int,
        acc: mutable.Builder[(String, Json), Vector[(String, Json)]]): Vector[(String, Json)] =
      if (i == 0) acc.result()
      else {
        val kj = unpack0(buffer)
        val vj = unpack0(buffer)
        kj.asString match {
          case Some(key) =>
            loop(i - 1, acc += (key -> vj))
          case None =>
            throw new Exception(s"Unpack map was failed. current position: $i")
        }
      }
    Json.fromFields(loop(size, Vector.newBuilder))
  }
}
