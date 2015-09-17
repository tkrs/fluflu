package fluflu

import java.io.IOException
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

  import java.nio.channels.NotYetConnectedException

  val ch = Channel(host, port, timeout)

  def write(ba: Array[Byte]): Task[Int] = Task delay {
    try {
      val bs = ByteBuffer wrap ba
      ch connect ()
      ch write bs
    } catch {
      case e: NotYetConnectedException => throw e
      case e: IOException =>
        ch.close()
        throw e
    }
  }

  def close() = ch close ()

}

object Channel {
  val m = scala.collection.concurrent.TrieMap[String, Channel]()
  def apply(host: String, port: Int, timeout: Int): Channel =
    m getOrElseUpdate (s"$host-$port", new Channel(host, port, timeout: Int))
}

class Channel(host: String, port: Int, timeout: Int) {

  import Channel._

  import java.net.{ InetSocketAddress, StandardSocketOptions }
  import java.nio.channels.SocketChannel
  import java.lang.{ Boolean => JBool }
  import StandardSocketOptions._

  private[this] val remote = new InetSocketAddress(host, port)

  private[this] var channel = SocketChannel open()

  def connect(): Unit =
    try {
      if (!channel.isConnected) reflesh()
    } catch {
      case e: IOException => throw e
    }

  def write(buf: ByteBuffer): Integer = channel write buf

  private[this] def reflesh(): Unit = channel synchronized {
    channel = SocketChannel open()
    channel setOption[JBool](SO_REUSEADDR, true)
    channel setOption[JBool](SO_KEEPALIVE, true)
    channel configureBlocking false
    channel connect remote
    channel finishConnect()
  }

  def close(): Unit = {
    m remove s"$host-$port"
    channel close()
  }

}
