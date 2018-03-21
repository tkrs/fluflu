package fluflu
package msgpack

import java.io.Closeable
import java.nio.CharBuffer
import java.nio.charset.{CharsetEncoder, StandardCharsets}
import java.time.Instant

import org.msgpack.core.MessagePack.Code
import org.msgpack.core.MessagePack.Code.{ARRAY16, ARRAY32, FIXARRAY_PREFIX}

import scala.util.control.NonFatal
import scala.util.{Failure, Success, Try}

trait Packer[A] {
  def apply(a: A): Either[Throwable, Array[Byte]]
}

object Packer {

  def apply[A](implicit P: Packer[A]): Packer[A] = P

  implicit val packString: Packer[String] = new Packer[String] {
    def apply(a: String): Either[Throwable, Array[Byte]] = {
      Right(Packer.formatStrFamily(a))
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
                      val dest = Array.ofDim[Byte](1 + v1.length + v2.length + v3.length)
                      dest(0) = 0x93.toByte
                      java.lang.System.arraycopy(v1, 0, dest, 1, v1.length)
                      java.lang.System.arraycopy(v2, 0, dest, 1 + v1.length, v2.length)
                      java.lang.System.arraycopy(v3, 0, dest, 1 + v1.length + v2.length, v3.length)
                      Right(dest)
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
            case Right(v1) =>
              A(a) match {
                case Left(e) => Left(e)
                case Right(v2) =>
                  val dest = Array.ofDim[Byte](1 + v1.length + v2.length)
                  dest(0) = 0x92.toByte
                  java.lang.System.arraycopy(v1, 0, dest, 1, v1.length)
                  java.lang.System.arraycopy(v2, 0, dest, 1 + v1.length, v2.length)
                  Right(dest)
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

  def formatUInt32(v: Long): Array[Byte] = {
    Array(
      (v >>> 24).toByte,
      (v >>> 16).toByte,
      (v >>> 8).toByte,
      (v >>> 0).toByte
    )
  }

  def formatStrFamilyHeader(sz: Int): Array[Byte] =
    if (sz < 32)
      Array((Code.FIXSTR_PREFIX | sz).toByte)
    else if (sz < 256)
      Array(Code.STR8, sz.toByte)
    else if (sz < 65536)
      Array(Code.STR16, (sz >>> 8).toByte, (sz >>> 0).toByte)
    else
      Array(Code.STR32,
            (sz >>> 24).toByte,
            (sz >>> 16).toByte,
            (sz >>> 8).toByte,
            (sz >>> 0).toByte)

  def formatStrFamily(v: String): Array[Byte] = {
    val cb   = CharBuffer.wrap(v)
    val buf  = encoder.get.encode(cb)
    val len  = buf.remaining()
    val h    = formatStrFamilyHeader(len)
    val dest = Array.ofDim[Byte](h.length + len)
    java.lang.System.arraycopy(h, 0, dest, 0, h.length)
    buf.get(dest, h.length, len)
    buf.clear()
    cb.clear()
    dest
  }

  def formatArrayHeader(arraySize: Int): Array[Byte] =
    if (arraySize < (1 << 4))
      Array((FIXARRAY_PREFIX | arraySize).toByte)
    else if (arraySize < (1 << 16))
      Array(ARRAY16, (arraySize >>> 8).toByte, (arraySize >>> 0).toByte)
    else
      Array(ARRAY32,
            (arraySize >>> 24).toByte,
            (arraySize >>> 16).toByte,
            (arraySize >>> 8).toByte,
            (arraySize >>> 0).toByte)
}
