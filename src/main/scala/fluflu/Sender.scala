package fluflu

import java.net.{ InetSocketAddress, StandardSocketOptions }
import java.nio.ByteBuffer
import java.nio.channels.SocketChannel

import scala.util.Try

trait Sender {
  def write(b: ByteBuffer): Long
  def write(bs: Array[ByteBuffer]): Long
  def close(): Unit
  def isConnected: Boolean
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

  private[this] var channel = SocketChannel.open()

  private[this] val remote = new InetSocketAddress(host, port)

  private[this] def connect(): Boolean = {
    Try {
      import java.lang.{ Boolean => JBool, Integer => JInt }
      import StandardSocketOptions._
      channel.socket().setSoTimeout(timeout)
      channel.setOption[JBool](SO_REUSEADDR, true);
      channel.setOption[JBool](SO_KEEPALIVE, true);
      channel.connect(remote)
    } recover {
      case e: Throwable =>
        e.printStackTrace()
        false
    } getOrElse (false)
  }

  connect()

  def write(b: ByteBuffer): Long = write(Array(b))

  def write(bs: Array[ByteBuffer]): Long = {
    channel.isConnected || {
      reflesh()
      connect()
    }
    channel.write(bs)
  }

  private[this] def reflesh() = { channel = SocketChannel.open() }

  def close() = channel.close()

  def isConnected = channel.isConnected

  override def toString = name

}
