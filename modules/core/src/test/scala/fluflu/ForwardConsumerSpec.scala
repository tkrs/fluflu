package fluflu

import java.nio.ByteBuffer
import java.util.concurrent.{ArrayBlockingQueue, Executors, ScheduledExecutorService}

import fluflu.msgpack._
import org.msgpack.core.{MessageBufferPacker, MessagePack, MessagePacker}
import org.scalatest.{BeforeAndAfterEach, FunSpec}

import scala.util.{Success, Try}

class ForwardConsumerSpec extends FunSpec with BeforeAndAfterEach with MsgpackHelper {

  type Elem = (String, MessageBufferPacker => Unit)

  var scheduler: ScheduledExecutorService = _
  var connection: Connection              = _

  implicit val packMOption: Packer[MOption] = new Packer[MOption] {
    def apply(a: MOption, packer: MessagePacker): Unit = ()
  }

  implicit val unpackAck: Unpacker[Option[Ack]] = new Unpacker[Option[Ack]] {
    def apply(bytes: ByteBuffer): Either[Throwable, Option[Ack]] = Right(Some(Ack("abc")))
  }

  override def beforeEach(): Unit = {
    scheduler = Executors.newSingleThreadScheduledExecutor()
    connection = new Connection {
      override def writeAndRead(message: ByteBuffer): Try[ByteBuffer] = Success(ByteBuffer.allocate(1))
      override def isClosed: Boolean                                  = false
      override def close(): Try[Unit]                                 = Success(())
    }
  }

  override def afterEach(): Unit = {
    scheduler.shutdownNow()
  }

  describe("consume") {
    it("should consume max-pulls messages") {
      val queue = new ArrayBlockingQueue[Elem](6)
      (1 to 6).foreach(_ => queue.offer(("tag", (m: MessageBufferPacker) => m.packNil())))
      val consumer = new ForwardConsumer(5, connection, queue)
      consumer.consume()
      assert(queue.size() === 1)
    }
  }

  describe("retrieveElements") {

    it("should create Map with tag as key") {
      val queue = new ArrayBlockingQueue[Elem](3)
      queue.offer(("a", (m: MessageBufferPacker) => m.writePayload(Array(1.toByte))))
      queue.offer(("b", (m: MessageBufferPacker) => m.writePayload(Array(2.toByte))))
      queue.offer(("b", (m: MessageBufferPacker) => m.writePayload(Array(3.toByte))))
      val consumer = new ForwardConsumer(5, connection, queue)
      val m        = consumer.retrieveElements()

      val p = MessagePack.DEFAULT_PACKER_CONFIG.newBufferPacker()

      {
        val a = m("a")
        assert(a.size === 1)
        val List(aa) = a.toList
        aa(p)
        assert(p.toByteArray === Array(1.toByte))
      }

      p.clear()

      {
        val b = m("b")
        assert(b.size === 2)
        val List(bb, bbb) = b.toList
        bb(p)
        assert(p.toByteArray === Array(2.toByte))
        p.clear()
        bbb(p)
        assert(p.toByteArray === Array(3.toByte))
      }
    }
  }
}
