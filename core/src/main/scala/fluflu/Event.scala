package fluflu

import java.time.Instant

import cats.syntax.either._
import fluflu.msgpack.MessagePacker
import io.circe.{ Encoder, Json }
import io.circe.syntax._

sealed trait Event[A]

object Event {

  def apply[A](
    prefix: String,
    label: String,
    record: A,
    time: Instant = Instant.now()): fluflu.Event[A] = Event(prefix, label, record, time)

  final case class Event[A](
    prefix: String,
    label: String,
    record: A,
    time: Instant = Instant.now()) extends fluflu.Event[A]

  final case class EventTime[A](
    prefix: String,
    label: String,
    record: A,
    time: Instant = Instant.now()) extends fluflu.Event[A]

  private[this] val packer = MessagePacker()

  private def formatUInt32(v: Long): Array[Byte] = {
    val arr = Array.ofDim[Byte](4)
    arr(0) = (v >>> 24).toByte
    arr(1) = (v >>> 16).toByte
    arr(2) = (v >>> 8).toByte
    arr(3) = (v >>> 0).toByte
    arr
  }

  implicit final class EventOps[A](private val e: fluflu.Event[A]) extends AnyVal {
    def pack(implicit A: Encoder[A]): Either[Throwable, Array[Byte]] = e match {
      case Event(prefix, label, record, time) =>
        packer pack (Json arr (
          Json fromString s"$prefix.$label",
          Json fromLong time.getEpochSecond,
          record.asJson))
      case EventTime(prefix, label, record, time) =>
        for {
          p <- packer.pack(Json.fromString(s"$prefix.$label"))
          s <- formatUInt32(time.getEpochSecond).asRight
          n <- formatUInt32(time.getNano.toLong).asRight
          r <- packer.pack(record.asJson)
        } yield Array(0x93.toByte) ++ p ++ Array(0xd7.toByte, 0x00.toByte) ++ s ++ n ++ r

    }
  }
}
