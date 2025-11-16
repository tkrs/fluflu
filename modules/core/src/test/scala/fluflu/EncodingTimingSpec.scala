package fluflu

import java.nio.ByteBuffer
import java.time.Instant
import java.util.concurrent.ConcurrentLinkedQueue

import fluflu.msgpack.*
import org.msgpack.core.MessagePacker
import org.scalatest.funspec.AnyFunSpec

import scala.util.Success

class EncodingTimingSpec extends AnyFunSpec {

  // Test case to demonstrate that encoding happens at emit time
  describe("Encoding timing") {
    it("should encode data at emit time, not at consume time") {
      // Mock connection that always succeeds
      implicit val connection: Connection = new Connection {
        override def writeAndRead(message: ByteBuffer): scala.util.Try[ByteBuffer] =
          Success(ByteBuffer.allocate(1))
        override def isClosed: Boolean             = false
        override def close(): scala.util.Try[Unit] = Success(())
      }

      // Valid packer instances for string and MOption
      implicit val packString: Packer[String] = new Packer[String] {
        def apply(a: String, packer: MessagePacker): Unit = packer.packString(a)
      }

      implicit val packInstant: Packer[Instant] = new Packer[Instant] {
        def apply(a: Instant, packer: MessagePacker): Unit = packer.packLong(a.toEpochMilli)
      }

      implicit val packMOption: Packer[MOption] = new Packer[MOption] {
        def apply(a: MOption, packer: MessagePacker): Unit = () // Simple mock
      }

      implicit val unpackAck: Unpacker[Option[Ack]] = new Unpacker[Option[Ack]] {
        def apply(bytes: ByteBuffer): Either[Throwable, Option[Ack]] = Right(Some(Ack("test")))
      }

      val client = Client()

      // This should succeed because the string is encodable
      val result1 = client.emit("test.tag", "valid string")
      assert(result1.isRight)
      assert(client.size > 0, "Queue should contain encoded message after emit")

      // Test that encoding errors are caught at emit time
      // Create a packer that always fails
      implicit val failingPacker: Packer[Int] = new Packer[Int] {
        def apply(a: Int, packer: MessagePacker): Unit =
          throw new RuntimeException("Encoding failed")
      }

      val result2 = client.emit("test.tag", 42)
      assert(result2.isLeft, "Should fail at emit time due to encoding error")
      assert(result2.left.toOption.get.getMessage.contains("Failed to encode message"))

      client.close()
    }

    it("should store encoded ByteBuffers in queue, not functions") {
      val queue = new ConcurrentLinkedQueue[(String, ByteBuffer)]()

      // Mock connection
      implicit val connection: Connection = new Connection {
        override def writeAndRead(message: ByteBuffer): scala.util.Try[ByteBuffer] =
          Success(ByteBuffer.allocate(1))
        override def isClosed: Boolean             = false
        override def close(): scala.util.Try[Unit] = Success(())
      }

      // Required implicit instances for ForwardConsumer
      implicit val packString: Packer[String] = new Packer[String] {
        def apply(a: String, packer: MessagePacker): Unit = packer.packString(a)
      }

      implicit val packMOption: Packer[MOption] = new Packer[MOption] {
        def apply(a: MOption, packer: MessagePacker): Unit = () // Simple mock
      }

      implicit val unpackAck: Unpacker[Option[Ack]] = new Unpacker[Option[Ack]] {
        def apply(bytes: ByteBuffer): Either[Throwable, Option[Ack]] = Right(Some(Ack("test")))
      }

      // Create a ForwardConsumer with our test queue
      val consumer = new ForwardConsumer(10, connection, queue)

      // Add some encoded data to the queue directly
      val testData = ByteBuffer.wrap("test data".getBytes())
      queue.offer("test.tag" -> testData)

      // Verify that the queue contains ByteBuffers, not functions
      val retrieved = consumer.retrieveElements()
      assert(retrieved.contains("test.tag"))
      assert(retrieved("test.tag").size === 1)

      val retrievedBuffer = retrieved("test.tag").head
      assert(retrievedBuffer.isInstanceOf[ByteBuffer])
    }
  }
}
