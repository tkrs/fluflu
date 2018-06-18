package fluflu.msgpack.shapes

import export.imports
import fluflu.msgpack.shapes.ast.MsgPack

import scala.annotation.tailrec
import scala.collection.generic.CanBuildFrom
import scala.collection.mutable

trait Decoder[A] extends Serializable { self =>

  def apply(m: MsgPack): Either[Throwable, A]

  final def map[B](f: A => B): Decoder[B] = new Decoder[B] {
    def apply(m: MsgPack): Either[Throwable, B] =
      self(m) match {
        case Right(v) => Right(f(v))
        case Left(e)  => Left(e)
      }
  }

  final def mapF[B](f: A => Either[Throwable, B]): Decoder[B] = new Decoder[B] {
    def apply(m: MsgPack): Either[Throwable, B] =
      self(m) match {
        case Right(v) => f(v)
        case Left(e)  => Left(e)
      }
  }

  final def flatMap[B](f: A => Decoder[B]): Decoder[B] = new Decoder[B] {
    def apply(m: MsgPack): Either[Throwable, B] =
      self(m) match {
        case Right(v) => f(v).apply(m)
        case Left(e)  => Left(e)
      }
  }
}

object Decoder extends LowPriorityDecoder {

  @inline def apply[A](implicit A: Decoder[A]): Decoder[A] = A

  implicit val decodeBoolean: Decoder[Boolean] = new Decoder[Boolean] {
    def apply(m: MsgPack): Either[Throwable, Boolean] = m match {
      case MsgPack.MBool(a) => Right(a)
      case _                => Left(new IllegalArgumentException(s"$m"))
    }
  }

  implicit val decodeBytes: Decoder[Byte] = new Decoder[Byte] {
    def apply(m: MsgPack): Either[Throwable, Byte] = m match {
      case MsgPack.MByte(a)   => Right(a)
      case MsgPack.MShort(a)  => Right(a.toByte)
      case MsgPack.MInt(a)    => Right(a.toByte)
      case MsgPack.MLong(a)   => Right(a.toByte)
      case MsgPack.MBigInt(a) => Right(a.toByte)
      case _                  => Left(new IllegalArgumentException(s"$m"))
    }
  }

  implicit val decodeShort: Decoder[Short] = new Decoder[Short] {
    def apply(m: MsgPack): Either[Throwable, Short] = m match {
      case MsgPack.MByte(a)   => Right(a.toShort)
      case MsgPack.MShort(a)  => Right(a)
      case MsgPack.MInt(a)    => Right(a.toShort)
      case MsgPack.MLong(a)   => Right(a.toShort)
      case MsgPack.MBigInt(a) => Right(a.toShort)
      case _                  => Left(new IllegalArgumentException(s"$m"))
    }
  }

  implicit val decodeInt: Decoder[Int] = new Decoder[Int] {
    def apply(m: MsgPack): Either[Throwable, Int] = m match {
      case MsgPack.MByte(a)   => Right(a.toInt)
      case MsgPack.MShort(a)  => Right(a.toInt)
      case MsgPack.MInt(a)    => Right(a)
      case MsgPack.MLong(a)   => Right(a.toInt)
      case MsgPack.MBigInt(a) => Right(a.toInt)
      case _                  => Left(new IllegalArgumentException(s"$m"))
    }
  }

  implicit val decodeLong: Decoder[Long] = new Decoder[Long] {
    def apply(m: MsgPack): Either[Throwable, Long] = m match {
      case MsgPack.MByte(a)   => Right(a.toLong)
      case MsgPack.MShort(a)  => Right(a.toLong)
      case MsgPack.MInt(a)    => Right(a.toLong)
      case MsgPack.MLong(a)   => Right(a)
      case MsgPack.MBigInt(a) => Right(a.toLong)
      case _                  => Left(new IllegalArgumentException(s"$m"))
    }
  }

  implicit val decodeBigInt: Decoder[BigInt] = new Decoder[BigInt] {
    def apply(m: MsgPack): Either[Throwable, BigInt] = m match {
      case MsgPack.MByte(a)   => Right(BigInt(a.toInt))
      case MsgPack.MShort(a)  => Right(BigInt(a.toInt))
      case MsgPack.MInt(a)    => Right(BigInt(a))
      case MsgPack.MLong(a)   => Right(BigInt(a))
      case MsgPack.MBigInt(a) => Right(a)
      case _                  => Left(new IllegalArgumentException(s"$m"))
    }
  }

  implicit val decodeDouble: Decoder[Double] = new Decoder[Double] {
    def apply(m: MsgPack): Either[Throwable, Double] = m match {
      case MsgPack.MFloat(a)  => Right(a.toDouble)
      case MsgPack.MDouble(a) => Right(a)
      case _                  => Left(new IllegalArgumentException(s"$m"))
    }
  }

  implicit val decodeFloat: Decoder[Float] = new Decoder[Float] {
    def apply(m: MsgPack): Either[Throwable, Float] = m match {
      case MsgPack.MFloat(a) => Right(a)
      case _                 => Left(new IllegalArgumentException(s"$m"))
    }
  }

  implicit val decodeChar: Decoder[Char] = new Decoder[Char] {
    def apply(m: MsgPack): Either[Throwable, Char] = m match {
      case MsgPack.MString(a) if a.length == 1 => Right(a.charAt(0))
      case _                                   => Left(new IllegalArgumentException(s"$m"))
    }
  }

  implicit val decodeString: Decoder[String] = new Decoder[String] {
    def apply(m: MsgPack): Either[Throwable, String] = m match {
      case MsgPack.MString(a) => Right(a)
      case _                  => Left(new IllegalArgumentException(s"$m"))
    }
  }

  implicit def decodeSome[A](implicit A: Decoder[A]): Decoder[Some[A]] =
    new Decoder[Some[A]] {
      def apply(m: MsgPack): Either[Throwable, Some[A]] =
        A(m) match {
          case Right(v) => Right(Some(v))
          case Left(e)  => Left(e)
        }
    }

  implicit val decodeNone: Decoder[None.type] =
    new Decoder[None.type] {
      def apply(m: MsgPack): Either[Throwable, None.type] = Right(None)
    }

  implicit def decodeOption[A](implicit A: Decoder[A]): Decoder[Option[A]] =
    new Decoder[Option[A]] {
      def apply(m: MsgPack): Either[Throwable, Option[A]] = m match {
        case MsgPack.MNil => Right(None)
        case _ =>
          A(m) match {
            case Right(v) => Right(Option(v))
            case Left(e)  => Left(e)
          }
      }
    }

  @inline private[this] def decodeContainer[C[_], A](
      implicit A: Decoder[A],
      cbf: CanBuildFrom[Nothing, A, C[A]]): Decoder[C[A]] =
    new Decoder[C[A]] {
      def apply(m: MsgPack): Either[Throwable, C[A]] = {
        @tailrec def loop(it: Iterator[MsgPack],
                          b: mutable.Builder[A, C[A]]): Either[Throwable, C[A]] = {
          if (!it.hasNext) Right(b.result())
          else {
            val a = it.next()
            A.apply(a) match {
              case Right(aa) =>
                loop(it, b += aa)
              case Left(e) =>
                Left(e)
            }
          }
        }

        m match {
          case MsgPack.MArray(a) =>
            val it = a.iterator
            loop(it, cbf.apply)
          case _ =>
            Left(new IllegalArgumentException(s"$m"))
        }
      }
    }

  implicit def decodeSeq[A](implicit A: Decoder[A]): Decoder[Seq[A]] = decodeContainer[Seq, A]

  implicit def decodeSet[A](implicit A: Decoder[A]): Decoder[Set[A]] = decodeContainer[Set, A]

  implicit def decodeList[A](implicit A: Decoder[A]): Decoder[List[A]] = decodeContainer[List, A]

  implicit def decodeVector[A](implicit A: Decoder[A]): Decoder[Vector[A]] =
    decodeContainer[Vector, A]

  implicit def decodeMapLike[M[_, _] <: Map[K, V], K, V](
      implicit
      K: Decoder[K],
      V: Decoder[V],
      cbf: CanBuildFrom[Nothing, (K, V), M[K, V]]): Decoder[M[K, V]] = new Decoder[M[K, V]] {
    def apply(m: MsgPack): Either[Throwable, M[K, V]] = {
      @tailrec def loop(it: Iterator[(MsgPack, MsgPack)],
                        b: mutable.Builder[(K, V), M[K, V]]): Either[Throwable, M[K, V]] = {
        if (!it.hasNext) Right(b.result())
        else {
          val (k, v) = it.next()
          K.apply(k) match {
            case Right(kk) =>
              V.apply(v) match {
                case Right(vv) =>
                  loop(it, b += kk -> vv)
                case Left(e) =>
                  Left(e)
              }
            case Left(e) =>
              Left(e)
          }
        }
      }

      m match {
        case MsgPack.MMap(a) =>
          val b = cbf.apply
          loop(a.iterator, b)
        case _ =>
          Left(new IllegalArgumentException(s"$m"))
      }
    }
  }
}

@imports[Decoder]
trait LowPriorityDecoder
