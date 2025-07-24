package fluflu

import java.nio.ByteBuffer
import java.util.concurrent.ArrayBlockingQueue
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService

import fluflu.msgpack.*
import org.msgpack.core.MessagePack
import org.msgpack.core.MessagePacker
import org.scalatest.BeforeAndAfterEach
import org.scalatest.funspec.AnyFunSpec

import scala.util.Success
import scala.util.Try

class ForwardConsumerSpec extends AnyFunSpec with BeforeAndAfterEach with MsgpackHelper {
  type Elem = (String, ByteBuffer)

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

  override def afterEach(): Unit =
    scheduler.shutdownNow()

  describe("consume") {
    it("should consume messages") {
      val queue  = new ArrayBlockingQueue[Elem](6)
      val packer = MessagePack.DEFAULT_PACKER_CONFIG.newBufferPacker()
      (1 to 6).foreach { _ =>
        packer.packNil()
        val buffer = packer.toMessageBuffer.sliceAsByteBuffer()
        queue.offer(("tag", buffer))
        packer.clear()
      }
      val consumer = new ForwardConsumer(10, connection, queue)
      consumer.consume()
      assert(queue.size() === 0)
    }
    it("should consume max-pulls messages") {
      val queue  = new ArrayBlockingQueue[Elem](6)
      val packer = MessagePack.DEFAULT_PACKER_CONFIG.newBufferPacker()
      (1 to 6).foreach { _ =>
        packer.packNil()
        val buffer = packer.toMessageBuffer.sliceAsByteBuffer()
        queue.offer(("tag", buffer))
        packer.clear()
      }
      val consumer = new ForwardConsumer(5, connection, queue)
      consumer.consume()
      assert(queue.size() === 1)
    }
  }

  describe("retrieveElements") {
    it("should create Map with tag as key") {
      val queue  = new ArrayBlockingQueue[Elem](3)
      val packer = MessagePack.DEFAULT_PACKER_CONFIG.newBufferPacker()

      // Create buffer for tag "a"
      packer.writePayload(Array(1.toByte))
      val bufferA = packer.toMessageBuffer.sliceAsByteBuffer()
      queue.offer(("a", bufferA))
      packer.clear()

      // Create buffer for tag "b" - first element
      packer.writePayload(Array(2.toByte))
      val bufferB1 = packer.toMessageBuffer.sliceAsByteBuffer()
      queue.offer(("b", bufferB1))
      packer.clear()

      // Create buffer for tag "b" - second element
      packer.writePayload(Array(3.toByte))
      val bufferB2 = packer.toMessageBuffer.sliceAsByteBuffer()
      queue.offer(("b", bufferB2))
      packer.clear()

      val consumer = new ForwardConsumer(5, connection, queue)
      val m        = consumer.retrieveElements()

      {
        val a = m("a")
        assert(a.size === 1)
        val List(aa) = a.toList
        assert(aa.array() === Array(1.toByte))
      }

      {
        val b = m("b")
        assert(b.size === 2)
        val List(bb, bbb) = b.toList
        assert(bb.array() === Array(2.toByte))
        assert(bbb.array() === Array(3.toByte))
      }
    }
  }
}
