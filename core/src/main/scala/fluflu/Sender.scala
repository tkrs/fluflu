package fluflu

import java.nio.ByteBuffer

import scalaz.concurrent.Task

trait Sender {
  def write(b: Array[Byte]): Task[Int]
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

  def write(ba: Array[Byte]): Task[Int] = Task delay {
    val bs = ByteBuffer wrap ba
    (ch write bs) run
  }

  def close() = ch close ()

}
