package fluflu.msgpack

import cats.Eq
import org.msgpack.core.{MessageBufferPacker, MessagePack}
import org.scalatest._

trait MsgpackHelper extends Matchers with BeforeAndAfterEach { self: Suite =>

  implicit val arrayEq: Eq[Array[Byte]] = Eq.instance[Array[Byte]](_.zip(_).forall {
    case (a, b) => a == b
  })

  var packer: MessageBufferPacker = _

  override def beforeEach(): Unit = {
    packer = MessagePack.DEFAULT_PACKER_CONFIG.newBufferPacker()
  }

  override def afterEach(): Unit = {
    packer.close()
  }

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
