package fluflu.msgpack

import org.msgpack.core.MessageBufferPacker
import org.msgpack.core.MessagePack
import org.scalatest.*
import org.scalatest.matchers.should.Matchers

trait MsgpackHelper extends Matchers with BeforeAndAfterEach { self: Suite =>

  var packer: MessageBufferPacker = _

  override def beforeEach(): Unit =
    packer = MessagePack.DEFAULT_PACKER_CONFIG.newBufferPacker()

  override def afterEach(): Unit =
    packer.close()
}

object MsgpackHelper {
  implicit class BinHelper(val sc: StringContext) extends AnyVal {
    def x(): Array[Byte] = {
      val strings = sc.parts.iterator

      def toByte(s: String): Byte = BigInt(s, 16).toByte

      strings.next().split(" ").map(toByte)
    }
  }
}
