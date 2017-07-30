package fluflu.queue

import java.nio.ByteBuffer
import java.time.{Clock, Duration}

import fluflu.{Backoff, Connection, Messenger}
import monix.eval.Task
import org.scalatest.{FunSpec, Matchers}

trait Suite extends FunSpec with Matchers {

  implicit val clock: Clock = Clock.systemUTC()

  implicit val connection: Connection = new Connection {
    override def isClosed: Boolean = false
    override def close(): Unit = ()
    override def write(message: ByteBuffer): Task[Unit] = Task.pure(())
  }

  implicit val messenger: Messenger =
    Messenger(Duration.ofMillis(10), Backoff.fix(Duration.ofMillis(10)))
}
