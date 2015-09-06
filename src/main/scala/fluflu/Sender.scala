package fluflu

import java.io.IOException
import java.net.{ InetSocketAddress, StandardSocketOptions }
import java.nio.ByteBuffer
import java.nio.channels.{ NotYetConnectedException, SocketChannel }

trait Sender {
  def write(b: ByteBuffer): Int
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

  def write(bs: ByteBuffer): Int = {
    val c = Channel.connect(remote, timeout)
    try {
      c.write(bs)
    } catch {
      case e: NotYetConnectedException =>
        throw e
      case e: IOException =>
        c.close()
        throw e
    }
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
      if (!channel.isConnectionPending) {
        val nc = reflesh()
        nc.configureBlocking(false)
        nc.socket().setSoTimeout(timeout)
        nc.setOption[JBool](SO_REUSEADDR, true)
        nc.setOption[JBool](SO_KEEPALIVE, true)
        nc.connect(remote)
        nc.finishConnect()
        nc
      } else {
        channel.finishConnect()
        channel
      }
    } else {
      channel
    }
  }

  private[this] def reflesh(): SocketChannel = {
    channel = SocketChannel.open()
    channel
  }

  def close(): Unit = {
    channel.close()
  }

}
