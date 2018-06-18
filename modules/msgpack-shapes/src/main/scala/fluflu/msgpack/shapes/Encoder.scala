package fluflu.msgpack.shapes

import export._
import fluflu.msgpack.shapes.ast.{MutMap, MsgPack}

import scala.annotation.tailrec
import scala.collection.mutable

trait Encoder[A] extends Serializable { self =>

  def apply(a: A): MsgPack

  final def contramap[B](f: B => A): Encoder[B] = new Encoder[B] {
    def apply(a: B): MsgPack = self(f(a))
  }
}

object Encoder extends LowPriorityEncoder {

  @inline def apply[A](implicit A: Encoder[A]): Encoder[A] = A

  final def instance[A](fa: A => MsgPack): Encoder[A] =
    new Encoder[A] {
      def apply(a: A): MsgPack = fa(a)
    }

  implicit final val encodeBoolean: Encoder[Boolean] = new Encoder[Boolean] {
    def apply(a: Boolean): MsgPack = MsgPack.MBool(a)
  }

  implicit final val encodeByte: Encoder[Byte] = new Encoder[Byte] {
    def apply(a: Byte): MsgPack = MsgPack.MByte(a)
  }

  implicit final val encodeShort: Encoder[Short] = new Encoder[Short] {
    def apply(a: Short): MsgPack = MsgPack.MShort(a)
  }

  implicit final val encodeInt: Encoder[Int] = new Encoder[Int] {
    def apply(a: Int): MsgPack = MsgPack.MInt(a)
  }

  implicit final val encodeLong: Encoder[Long] = new Encoder[Long] {
    def apply(a: Long): MsgPack = MsgPack.MLong(a)
  }

  implicit final val encodeBigInt: Encoder[BigInt] = new Encoder[BigInt] {
    def apply(a: BigInt): MsgPack = MsgPack.MBigInt(a)
  }

  implicit final val encodeDouble: Encoder[Double] = new Encoder[Double] {
    def apply(a: Double): MsgPack = MsgPack.MDouble(a)
  }

  implicit final val encodeFloat: Encoder[Float] = new Encoder[Float] {
    def apply(a: Float): MsgPack = MsgPack.MFloat(a)
  }

  implicit final val encodeChar: Encoder[Char] = new Encoder[Char] {
    def apply(a: Char): MsgPack = MsgPack.MString(a.toString)
  }

  implicit final val encodeString: Encoder[String] = new Encoder[String] {
    def apply(a: String): MsgPack = MsgPack.MString(a)
  }

  implicit final def encodeSymbol[K <: Symbol]: Encoder[K] = new Encoder[K] {
    def apply(a: K): MsgPack = MsgPack.MString(a.name)
  }

  implicit final def encodeOption[A](implicit A: Encoder[A]): Encoder[Option[A]] =
    new Encoder[Option[A]] {
      def apply(a: Option[A]): MsgPack = a match {
        case Some(v) => A(v)
        case None    => MsgPack.MNil
      }
    }

  implicit final def encodeSome[A](implicit A: Encoder[A]): Encoder[Some[A]] =
    A.contramap[Some[A]](_.get)

  implicit final val encodeNone: Encoder[None.type] = new Encoder[None.type] {
    def apply(a: None.type): MsgPack = MsgPack.MNil
  }

  @tailrec private[this] def iterLoop[A](
      rem: Iterator[A],
      acc: mutable.Builder[MsgPack, Vector[MsgPack]])(implicit A: Encoder[A]): Vector[MsgPack] =
    if (!rem.hasNext) acc.result()
    else iterLoop(rem, acc += A(rem.next()))

  implicit final def encodeSeq[A: Encoder]: Encoder[Seq[A]] = new Encoder[Seq[A]] {
    def apply(a: Seq[A]): MsgPack = {
      MsgPack.MArray(iterLoop(a.iterator, Vector.newBuilder))
    }
  }

  implicit final def encodeSet[A: Encoder]: Encoder[Set[A]] = new Encoder[Set[A]] {
    def apply(a: Set[A]): MsgPack = {
      MsgPack.MArray(iterLoop(a.iterator, Vector.newBuilder))
    }
  }

  implicit final def encodeList[A: Encoder]: Encoder[List[A]] = new Encoder[List[A]] {
    def apply(a: List[A]): MsgPack = {
      MsgPack.MArray(iterLoop(a.iterator, Vector.newBuilder))
    }
  }

  implicit final def encodeVector[A: Encoder]: Encoder[Vector[A]] = new Encoder[Vector[A]] {
    def apply(a: Vector[A]): MsgPack = {
      MsgPack.MArray(iterLoop(a.iterator, Vector.newBuilder))
    }
  }

  @tailrec private[this] def mapLoop[K, V](it: Iterator[(K, V)], acc: MutMap)(
      implicit
      K: Encoder[K],
      V: Encoder[V]): MutMap = {
    if (!it.hasNext) acc
    else {
      val (k, v) = it.next()
      mapLoop(it, acc.add(K(k), V(v)))
    }
  }

  implicit final def encodeMapLike[M[k, +v] <: Map[K, V], K, V](implicit
                                                                K: Encoder[K],
                                                                V: Encoder[V]): Encoder[M[K, V]] =
    new Encoder[M[K, V]] {
      def apply(a: M[K, V]): MsgPack =
        MsgPack.MMap(mapLoop(a.iterator, MutMap.empty))
    }

  implicit final def encodeMap[K, V](implicit
                                     K: Encoder[K],
                                     V: Encoder[V]): Encoder[Map[K, V]] =
    new Encoder[Map[K, V]] {
      def apply(a: Map[K, V]): MsgPack =
        MsgPack.MMap(mapLoop(a.iterator, MutMap.empty))
    }
}

@imports[Encoder]
trait LowPriorityEncoder
