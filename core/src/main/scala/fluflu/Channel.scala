package fluflu

import java.nio.ByteBuffer
import java.nio.channels._
import java.net.{ InetSocketAddress, StandardSocketOptions }
import java.lang.{ Boolean => JBool }
import java.util.concurrent.atomic.AtomicReference

import scala.compat.java8.FunctionConverters._
import scala.concurrent.{ ExecutionContext, Future }

object Channel {
  private[fluflu] val channelMap = scala.collection.concurrent.TrieMap[String, Channel]()
  def apply(host: String, port: Int, timeout: Int): Channel =
    channelMap getOrElseUpdate (s"$host-$port", new Channel(host, port, timeout: Int))
}

class Channel(host: String, port: Int, timeout: Int) {

  import Channel._
  import StandardSocketOptions._

  private[this] val remote = new InetSocketAddress(host, port)

  private[this] val channel: AtomicReference[SocketChannel] =
    new AtomicReference(SocketChannel.open())

  def connect(reset: Boolean = false): Unit = {
    channel.getAndUpdate(asJavaUnaryOperator { x =>
      val c = if (x == null || reset) SocketChannel.open() else x
      c.configureBlocking(true)
      c.setOption[JBool](TCP_NODELAY, true)
      c.connect(remote)
      c
    })
  }

  def write(buf: ByteBuffer)(implicit ec: ExecutionContext): Future[Int] =
    Future(channel.get.write(buf))

  def close(): Unit = {
    channel.get.close()
    channelMap.remove(s"$host-$port")
  }
}
