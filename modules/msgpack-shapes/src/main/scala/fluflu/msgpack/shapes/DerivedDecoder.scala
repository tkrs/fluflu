package fluflu.msgpack.shapes

import export._
import fluflu.msgpack.shapes.ast.MsgPack
import shapeless._
import shapeless.labelled.{field, FieldType}

trait DerivedDecoder[A] extends Decoder[A]

@exports
object DerivedDecoder extends LowPriorityDerivedDecoder

trait LowPriorityDerivedDecoder {

  implicit final val decodeHNil: DerivedDecoder[HNil] = new DerivedDecoder[HNil] {
    def apply(a: MsgPack): Either[Throwable, HNil] = Right(HNil)
  }

  implicit final def decodeLabelledHList[K <: Symbol, H, T <: HList](
      implicit
      K: Witness.Aux[K],
      H: Decoder[H],
      T: DerivedDecoder[T]): DerivedDecoder[FieldType[K, H] :: T] =
    new DerivedDecoder[FieldType[K, H] :: T] {
      def apply(m: MsgPack): Either[Throwable, FieldType[K, H] :: T] = m match {
        case MsgPack.MMap(a) =>
          a.get(MsgPack.MString(K.value.name)) match {
            case Some(v) =>
              T(m) match {
                case Right(t) =>
                  H(v) match {
                    case Right(h) =>
                      Right(field[K](h) :: t)
                    case Left(e) =>
                      Left(e)
                  }
                case Left(e) =>
                  Left(e)
              }
            case None =>
              Left(new IllegalArgumentException)
          }
        case _ => Left(new IllegalArgumentException("Uncaught ..."))
      }
    }

  implicit final def decodeGen[A, R](implicit
                                     gen: LabelledGeneric.Aux[A, R],
                                     R: Lazy[DerivedDecoder[R]]): DerivedDecoder[A] =
    new DerivedDecoder[A] {
      def apply(a: MsgPack): Either[Throwable, A] = R.value(a) match {
        case Right(v) => Right(gen.from(v))
        case Left(e)  => Left(e)
      }
    }
}
