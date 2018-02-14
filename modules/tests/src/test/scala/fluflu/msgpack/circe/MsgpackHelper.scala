package fluflu.msgpack.circe

import cats.Eq
import org.scalatest.Matchers

trait MsgpackHelper extends Matchers {

  implicit val arrayEq: Eq[Array[Byte]] = Eq.instance[Array[Byte]](_.zip(_).forall {
    case (a, b) => a == b
  })
}
