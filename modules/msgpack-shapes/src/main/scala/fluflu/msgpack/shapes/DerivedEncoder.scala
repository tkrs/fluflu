package fluflu.msgpack.shapes

import export._
import fluflu.msgpack.shapes.ast.{MutMap, MsgPack}
import shapeless._
import shapeless.labelled.FieldType

trait DerivedEncoder[A] extends Encoder[A]

@exports
object DerivedEncoder extends LowPriorityDerivedEncoder

trait LowPriorityDerivedEncoder {

  implicit final val encodeHNil: DerivedEncoder[HNil] = new DerivedEncoder[HNil] {
    def apply(a: HNil): MsgPack = MsgPack.MMap(MutMap.empty)
  }

  implicit final def encodeLabelledHList[K <: Symbol, H, T <: HList](
      implicit
      K: Witness.Aux[K],
      S: Encoder[K],
      H: Encoder[H],
      T: DerivedEncoder[T]): DerivedEncoder[FieldType[K, H] :: T] =
    new DerivedEncoder[FieldType[K, H] :: T] {
      def apply(a: FieldType[K, H] :: T): MsgPack =
        T(a.tail).add(S(K.value), H(a.head))
    }

  implicit final def encodeGen[A, R](implicit
                                     gen: LabelledGeneric.Aux[A, R],
                                     R: Lazy[DerivedEncoder[R]]): DerivedEncoder[A] =
    new DerivedEncoder[A] {
      def apply(a: A): MsgPack = R.value(gen.to(a))
    }
}
