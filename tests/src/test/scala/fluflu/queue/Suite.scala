package fluflu.queue

import java.nio.ByteBuffer
import java.time.Clock

import fluflu.{Connection, Messenger}
import org.scalatest.{FunSpec, Matchers}

import scala.util.Try

trait Suite extends FunSpec with Matchers {

  implicit val clock: Clock = Clock.systemUTC()

  implicit val connection: Connection = new Connection {
    override def isClosed: Boolean                     = false
    override def close(): Unit                         = ()
    override def write(message: ByteBuffer): Try[Unit] = Try(())
  }

  implicit val messenger: Messenger = Messenger.noop
}
