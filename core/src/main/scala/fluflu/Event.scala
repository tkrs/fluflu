package fluflu

import java.time.Instant

import fluflu.msgpack.Packer

import scala.collection.mutable

sealed trait Event[A]

object Event {

  def apply[A](prefix: String,
               label: String,
               record: A,
               time: Instant = Instant.now()): fluflu.Event[A] =
    Event(prefix, label, record, time)

  final case class Event[A](prefix: String, label: String, record: A, time: Instant = Instant.now())
      extends fluflu.Event[A]

  final case class EventTime[A](prefix: String,
                                label: String,
                                record: A,
                                time: Instant = Instant.now())
      extends fluflu.Event[A]

  implicit def eventPacker[A](implicit A: Packer[A]): Packer[fluflu.Event[A]] =
    new Packer[fluflu.Event[A]] {
      override def apply(a: fluflu.Event[A]): Either[Throwable, Array[Byte]] =
        try a match {
          case Event(prefix, label, record, time) =>
            val acc = mutable.ArrayBuilder.make[Byte]
            acc += 0x93.toByte
            Packer.formatStrFamily(s"$prefix.$label", acc)
            Packer.formatIntFamily(time.getEpochSecond, acc)
            A(record) match {
              case Right(arr) =>
                acc ++= arr
                Right(acc.result())
              case l => l
            }

          case EventTime(prefix, label, record, time) =>
            val acc = mutable.ArrayBuilder.make[Byte]
            acc += 0x93.toByte
            Packer.formatStrFamily(s"$prefix.$label", acc)
            acc += 0xd7.toByte
            acc += 0x00.toByte
            Packer.formatUInt32(time.getEpochSecond, acc)
            Packer.formatUInt32(time.getNano.toLong, acc)
            A(record) match {
              case Right(arr) =>
                acc ++= arr
                Right(acc.result())
              case l => l
            }
        } catch {
          case e: Throwable => Left(e)
        }
    }
}
