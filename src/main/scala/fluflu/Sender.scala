package fluflu

import java.io.IOException
import java.net.{ InetSocketAddress, StandardSocketOptions }
import java.nio.ByteBuffer
import java.nio.channels.SocketChannel

trait Sender {
  def write(b: ByteBuffer): Long
  def write(bs: Array[ByteBuffer]): Long
  def close(): Unit
}

object DefaultSender {
  def apply(
    host: String = "localhost",
    port: Int = 24224,
    timeout: Int = 3 * 1000
  ): Sender = new DefaultSender(host, port, timeout)
}

class DefaultSender(
    val host: String,
    val port: Int,
    val timeout: Int
) extends Sender {

  val name = s"${host}_${port}"

  private[this] val remote = new InetSocketAddress(host, port)

  val _ = Channel.connect(remote, timeout)

  def write(b: ByteBuffer): Long = write(Array(b))

  def write(bs: Array[ByteBuffer]): Long = {
    val c = Channel.connect(remote, timeout)
    try { c.write(bs) } catch { case e: IOException => c.close(); throw e }
  }

  def close() = Channel.close()

  override def toString = name

}

object Channel {

  private[this] var channel = SocketChannel.open()

  def connect(remote: InetSocketAddress, timeout: Int): SocketChannel = synchronized {

    import java.lang.{ Boolean => JBool }
    import StandardSocketOptions._

    if (!channel.isConnected) {
      val nc = reflesh()
      nc.socket().setSoTimeout(timeout)
      nc.setOption[JBool](SO_REUSEADDR, true)
      nc.setOption[JBool](SO_KEEPALIVE, true)
      nc.connect(remote)
      nc
    } else {
      channel
    }
  }

  private[this] def reflesh(): SocketChannel = {
    val c = SocketChannel.open()
    channel = c
    channel
  }

  def close(): Unit = channel.close()

}
