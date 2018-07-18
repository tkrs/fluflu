package fluflu

import java.nio.ByteBuffer
import java.time.Clock

import org.scalatest.{FunSpec, Matchers}

import scala.util.{Success, Try}

trait Suite extends FunSpec with Matchers {

  implicit val clock: Clock = Clock.systemUTC()

  implicit val connection: Connection = new Connection {
    override def isClosed: Boolean                                  = false
    override def close(): Try[Unit]                                 = Success(())
    override def writeAndRead(message: ByteBuffer): Try[ByteBuffer] = Try(ByteBuffer.wrap(Array(1, 2, 4).map(_.toByte)))
  }

}
