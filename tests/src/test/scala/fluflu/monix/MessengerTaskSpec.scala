package fluflu.monix

import java.io.IOException
import java.nio.ByteBuffer
import java.time.{Clock, Duration, Instant, ZoneId}
import java.util.concurrent.{ArrayBlockingQueue, BlockingQueue}

import _root_.monix.execution.Scheduler
import fluflu.{Backoff, Connection, Messenger}
import org.scalatest.{FunSpec, Matchers}

import scala.util.{Failure, Success, Try}

class MessengerTaskSpec extends FunSpec with Matchers {

  implicit val clock: Clock = Clock.fixed(Instant.now, ZoneId.systemDefault())

  describe("write") {
    it("should write successfully") {
      implicit val scheduler: Scheduler = Scheduler.singleThread(name = "test")

      implicit val connection: Connection = new Connection {
        val queue: BlockingQueue[Try[Unit]] = new ArrayBlockingQueue[Try[Unit]](5)
        queue.add(Failure(new IOException("🥂")))
        queue.add(Failure(new IOException("🥂")))
        queue.add(Success(()))

        override def isClosed: Boolean                     = false
        override def close(): Try[Unit]                    = Success(())
        override def write(message: ByteBuffer): Try[Unit] = queue.take()
      }

      val messenger: Messenger =
        new MessengerTask(Duration.ofMillis(200), Backoff.fix(Duration.ofMillis(10)))

      messenger.emit(Iterator(() => Right(Array(1, 2, 3).map(_.toByte))))
    }
  }
}
