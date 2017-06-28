package fluflu

import java.io.IOException
import java.nio.ByteBuffer
import java.time.{ Clock, Duration, Instant, ZoneId }
import java.util.concurrent.{ ArrayBlockingQueue, BlockingQueue }

import monix.eval.Task
import monix.execution.Scheduler
import org.scalatest.{ FunSpec, Matchers }

import scala.concurrent.Await
import scala.concurrent.duration.{ Duration => SDuration }

class MessengerSpec extends FunSpec with Matchers {

  implicit val clock: Clock = Clock.fixed(Instant.now, ZoneId.systemDefault())

  describe("write") {
    it("should write successfully") {
      implicit val scheduler: Scheduler = Scheduler.singleThread(name = "test")

      implicit val connection: Connection = new Connection {
        val queue: BlockingQueue[Task[Unit]] = new ArrayBlockingQueue[Task[Unit]](5)
        queue.add(Task.raiseError(new IOException("🥂")))
        queue.add(Task.raiseError(new IOException("🥂")))
        queue.add(Task.pure(()))

        override def isClosed: Boolean = false
        override def close(): Unit = ()
        override def write(message: ByteBuffer): Task[Unit] = queue.take()
      }

      val messenger: Messenger =
        Messenger(Duration.ofMillis(200), Backoff.fix(Duration.ofMillis(10)))

      Await.result(messenger.write(Letter(Array(1, 2, 3).map(_.toByte))).runAsync, SDuration.Inf)
    }
  }
}
