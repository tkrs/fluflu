package fluflu.queue

import java.time.Instant
import java.util.concurrent.{ArrayBlockingQueue, Executors}

import fluflu.msgpack.Packer
import org.scalatest.FunSpec

class ProducerSpec extends FunSpec {
  import fluflu.time.eventTime._

  implicit val packInt: Packer[Int] = new Packer[Int] {
    def apply(a: Int): Either[Throwable, Array[Byte]] = Right(Array(0x10.toByte))
  }

  describe("emit") {
    it("should reject message when its queue was full") {
      val queue     = new ArrayBlockingQueue[() => Either[Throwable, Array[Byte]]](1)
      val scheduler = Executors.newSingleThreadScheduledExecutor()
      queue.offer(() => Right(Array.emptyByteArray))
      val producer = new Producer(scheduler, queue)
      val l        = producer.emit("tag", 10, Instant.MAX)
      assert(l.isLeft)
    }
    it("should reject message when scheduler was shutdown") {
      val queue     = new ArrayBlockingQueue[() => Either[Throwable, Array[Byte]]](2)
      val scheduler = Executors.newSingleThreadScheduledExecutor()
      scheduler.shutdownNow()
      queue.offer(() => Right(Array.emptyByteArray))
      val producer = new Producer(scheduler, queue)
      val l        = producer.emit("tag", 10, Instant.MAX)
      assert(l.isLeft)
    }
  }
}
