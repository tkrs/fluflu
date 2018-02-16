package fluflu.msgpack

import cats.Eq
import org.scalatest._

trait MsgpackHelper extends Matchers {

  implicit val arrayEq: Eq[Array[Byte]] = Eq.instance[Array[Byte]](_.zip(_).forall {
    case (a, b) => a == b
  })

}

object MsgpackHelper {

  implicit class BinHelper(val sc: StringContext) extends AnyVal {
    def x(args: Any*): Array[Byte] = {
      val strings = sc.parts.iterator

      def toByte(s: String): Byte = BigInt(s, 16).toByte

      strings.next.split(" ").map(toByte)
    }
  }
}
