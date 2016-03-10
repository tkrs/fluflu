package fluflu

import java.nio.ByteBuffer

import scala.concurrent.Future

trait Sender {
  def write(b: Array[Byte]): Future[Int]
  def close(): Unit
}

object DefaultSender {
  def apply(
    host: String = "localhost",
    port: Int = 24224,
    timeout: Int = 3 * 1000,
    bufferCapacity: Int = 2 * 1024 * 1024
  ): Sender = new DefaultSender(host, port, timeout)
}

class DefaultSender(
    host: String,
    port: Int,
    timeout: Int
) extends Sender {

  val ch = Channel(host, port, timeout)
  ch connect ()

  def write(ba: Array[Byte]): Future[Int] =
    ch write (ByteBuffer wrap ba)

  def close() = ch close ()

}
