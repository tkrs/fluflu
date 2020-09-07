package fluflu

import java.nio.ByteBuffer
import java.time.Clock

import org.scalatest.funspec.AnyFunSpec
import org.scalatest.matchers.should.Matchers

import scala.util.Success
import scala.util.Try

trait Suite extends AnyFunSpec with Matchers {
  implicit val clock: Clock = Clock.systemUTC()

  implicit val connection: Connection = new Connection {
    override def isClosed: Boolean                                  = false
    override def close(): Try[Unit]                                 = Success(())
    override def writeAndRead(message: ByteBuffer): Try[ByteBuffer] = Try(ByteBuffer.wrap(Array(1, 2, 4).map(_.toByte)))
  }
}
