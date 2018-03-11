package fluflu
package msgpack

import java.io.Closeable
import java.nio.CharBuffer
import java.nio.charset.{CharsetEncoder, StandardCharsets}
import java.time.Instant

import org.msgpack.core.MessagePack.Code
import org.msgpack.core.MessagePack.Code.{ARRAY16, ARRAY32, FIXARRAY_PREFIX}

import scala.collection.mutable
import scala.util.control.NonFatal
import scala.util.{Failure, Success, Try}

trait Packer[A] {
  def apply(a: A): Either[Throwable, Array[Byte]]
}

object Packer {

  def apply[A](implicit P: Packer[A]): Packer[A] = P

  implicit val packString: Packer[String] = new Packer[String] {
    def apply(a: String): Either[Throwable, Array[Byte]] = {
      val acc = mutable.ArrayBuilder.make[Byte]
      Packer.formatStrFamily(a, acc)
      Right(acc.result())
    }
  }

  implicit def packEvent[A](implicit
                            S: Packer[String],
                            A: Packer[A],
                            T: Packer[Instant]): Packer[(String, A, Instant)] =
    new Packer[(String, A, Instant)] {
      def apply(v: (String, A, Instant)): Either[Throwable, Array[Byte]] = v match {
        case (s, a, t) =>
          S(s) match {
            case Left(e) => Left(e)
            case Right(v1) =>
              T(t) match {
                case Left(e) => Left(e)
                case Right(v2) =>
                  A(a) match {
                    case Left(e) => Left(e)
                    case Right(v3) =>
                      val acc = mutable.ArrayBuilder.make[Byte]
                      acc += 0x93.toByte
                      acc ++= v1
                      acc ++= v2
                      acc ++= v3
                      Right(acc.result())
                  }
              }
          }
      }
    }

  implicit def packEntry[A](implicit
                            A: Packer[A],
                            T: Packer[Instant]): Packer[(A, Instant)] =
    new Packer[(A, Instant)] {
      def apply(v: (A, Instant)): Either[Throwable, Array[Byte]] = v match {
        case (a, t) =>
          T(t) match {
            case Left(e) => Left(e)
            case Right(v2) =>
              A(a) match {
                case Left(e) => Left(e)
                case Right(v3) =>
                  val acc = mutable.ArrayBuilder.make[Byte]
                  acc += 0x92.toByte
                  acc ++= v2
                  acc ++= v3
                  Right(acc.result())
              }
          }
      }
    }

  private[this] val encoder: ThreadLocal[CharsetEncoder] =
    new ThreadLocal[CharsetEncoder] {
      override def initialValue(): CharsetEncoder =
        StandardCharsets.UTF_8.newEncoder()
    }

  def using[R <: Closeable, A](get: Try[R])(fr: R => Try[A]): Try[A] =
    get match {
      case Success(r) =>
        fr(r)
          .map { a =>
            r.close(); a
          }
          .recoverWith {
            case NonFatal(e) =>
              try r.close()
              catch {
                case NonFatal(_) => ()
              }
              Failure(e)
          }
      case Failure(e) => Failure(e)
    }

  def formatUInt32(v: Long, builder: mutable.ArrayBuilder[Byte]): Unit = {
    builder += (v >>> 24).toByte
    builder += (v >>> 16).toByte
    builder += (v >>> 8).toByte
    builder += (v >>> 0).toByte
  }

  def formatStrFamilyHeader(sz: Int, builder: mutable.ArrayBuilder[Byte]): Unit =
    if (sz < 32)
      builder += (Code.FIXSTR_PREFIX | sz).toByte
    else if (sz < 256) {
      builder += Code.STR8
      builder += sz.toByte
    } else if (sz < 65536) {
      builder += Code.STR16
      builder += (sz >>> 8).toByte
      builder += (sz >>> 0).toByte
    } else {
      builder += Code.STR32
      builder += (sz >>> 24).toByte
      builder += (sz >>> 16).toByte
      builder += (sz >>> 8).toByte
      builder += (sz >>> 0).toByte
    }

  def formatStrFamily(v: String, builder: mutable.ArrayBuilder[Byte]): Unit = {
    val cb  = CharBuffer.wrap(v)
    val buf = encoder.get.encode(cb)
    val len = buf.remaining()
    formatStrFamilyHeader(len, builder)
    val arr = Array.ofDim[Byte](len)
    buf.get(arr)
    builder ++= arr
    buf.clear()
    cb.clear()
  }

  def formatArrayHeader(arraySize: Int, builder: mutable.ArrayBuilder[Byte]): Unit = {
    if (arraySize < (1 << 4)) {
      builder += (FIXARRAY_PREFIX | arraySize).toByte
    } else if (arraySize < (1 << 16)) {
      builder += ARRAY16
      builder += (arraySize >>> 8).toByte
      builder += (arraySize >>> 0).toByte
    } else {
      builder += ARRAY32
      builder += (arraySize >>> 24).toByte
      builder += (arraySize >>> 16).toByte
      builder += (arraySize >>> 8).toByte
      builder += (arraySize >>> 0).toByte
    }
  }
}
