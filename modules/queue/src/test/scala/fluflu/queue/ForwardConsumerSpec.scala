package fluflu.queue

import java.time.Duration
import java.util.concurrent.{ArrayBlockingQueue, Executors, ScheduledExecutorService}

import fluflu.Messenger
import org.scalatest.{BeforeAndAfterEach, FunSpec}

class ForwardConsumerSpec extends FunSpec with BeforeAndAfterEach {
  import fluflu.msgpack.MsgpackHelper._

  type Elem = () => (String, Either[Throwable, Array[Byte]])

  var scheduler: ScheduledExecutorService = _
  var messenger: Messenger                = _

  override def beforeEach(): Unit = {
    scheduler = Executors.newSingleThreadScheduledExecutor()
    messenger = new Messenger {
      def emit(elms: Iterator[Array[Byte]]): Unit = elms.foreach(_ => ())
      def close(): Unit                           = ()
    }
  }

  override def afterEach(): Unit = {
    scheduler.shutdownNow()
  }

  describe("consume") {
    it("should consume max-pulls messages") {
      val queue = new ArrayBlockingQueue[Elem](6)
      (1 to 6).foreach(_ => queue.offer(() => ("tag", Right(Array(0x01.toByte)))))
      val consumer = new ForwardConsumer(Duration.ofMillis(1), 5, messenger, scheduler, queue)
      consumer.consume()
      assert(queue.size() === 1)
    }
  }

  describe("mkMap") {

    it("should create Map with tag as key") {
      val queue = new ArrayBlockingQueue[Elem](3)
      queue.offer(() => ("a", Right(Array(0x01.toByte))))
      (1 to 2).foreach(_ => queue.offer(() => ("b", Right(Array(0x02.toByte)))))
      val consumer = new ForwardConsumer(Duration.ofMillis(1), 5, messenger, scheduler, queue)
      val m        = consumer.mkMap

      {
        val (a, sz)  = m("a")
        val List(aa) = a.toList
        assert(sz === 1)
        assert(aa === Array(1.toByte))
      }

      {
        val (b, sz)       = m("b")
        val List(bb, bbb) = b.toList
        assert(sz === 2)
        assert(bb === Array(2.toByte))
        assert(bbb === Array(2.toByte))
      }
    }
  }

  describe("mkBuffers") {

    it("should create buffers as ForwardMode format") {
      val queue = new ArrayBlockingQueue[Elem](3)
      queue.offer(() => ("a", Right(Array(0x01.toByte))))
      (1 to 2).foreach(_ => queue.offer(() => ("b", Right(Array(0x02.toByte)))))
      val consumer     = new ForwardConsumer(Duration.ofMillis(1), 5, messenger, scheduler, queue)
      val m            = consumer.mkMap
      val List(r1, r2) = consumer.mkBuffers(m).toList.sortBy(_.length)
      assert(r1 === x"92 a1 61 91 01")
      assert(r2 === x"92 a1 62 92 02 02")
    }
  }
}
