package fluflu.queue

import java.time.Duration
import java.util.concurrent.{ArrayBlockingQueue, Executors}

import fluflu.Messenger
import org.scalatest.{BeforeAndAfterEach, FunSpec}

class ConsumerSpec extends FunSpec with BeforeAndAfterEach {

  type Elem = () => Array[Byte]

  var consumer: Consumer           = _
  var messenger: Messenger         = _
  var queue: java.util.Queue[Elem] = _

  override def beforeEach(): Unit = {
    val scheduler = Executors.newSingleThreadScheduledExecutor()
    queue = new ArrayBlockingQueue[Elem](10)
    messenger = new Messenger {
      def emit(elms: Iterator[Array[Byte]]): Unit = elms.foreach(_ => ())
      def close(): Unit                           = ()
    }
    consumer = new DefaultConsumer(Duration.ofMillis(1), 5, messenger, scheduler, queue)
  }

  override def afterEach(): Unit = {}

  describe("consume") {
    it("should consume max-pulls messages") {
      (1 to 6).foreach(_ => queue.offer(() => Array(0x01.toByte)))
      consumer.consume()
      assert(queue.size() === 1)
    }
  }
}
