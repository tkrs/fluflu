package fluflu

import java.io.IOException
import java.lang.{ Boolean => JBool }
import java.net.{ InetSocketAddress, StandardSocketOptions }
import java.nio.ByteBuffer
import java.nio.channels.{ NotYetConnectedException, SocketChannel }
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference

import scala.compat.java8.FunctionConverters._
import scala.concurrent.blocking
import scala.util.{ Failure, Try }

final case class Connection(remote: InetSocketAddress, maxConnectRetries: Int, backoff: Backoff) {
  import StandardSocketOptions._

  private[this] val channel: AtomicReference[SocketChannel] =
    new AtomicReference(null)

  @volatile private[fluflu] var noLongerRetriable: Boolean = false

  private[this] def open = {
    val ch = SocketChannel.open()
    ch.setOption[JBool](TCP_NODELAY, true)
    ch
  }

  def connect(): Unit =
    channel.updateAndGet(asJavaUnaryOperator { c =>
      var x = if (c == null) { open } else c
      def doConnect(retries: Int): SocketChannel = {
        var retries = 0
        while (retries < maxConnectRetries) {
          try {
            if (x.connect(remote)) return x else retries += 1
          } catch {
            case e: IOException =>
              blocking {
                TimeUnit.NANOSECONDS.sleep(backoff.nextDelay(retries).toNanos)
              }
              x.close()
              x = open
              retries += 1
          }
        }
        if (retries == maxConnectRetries) {
          if (x.isOpen) x.close()
          noLongerRetriable = true
          return null
        }
        x
      }
      doConnect(0)
    })

  connect()

  def isClosed: Boolean = channel.get == null

  def write(message: ByteBuffer): Unit =
    Try(channel.get.write(message)) recoverWith {
      case e: NotYetConnectedException =>
        channel.get.finishConnect()
        Failure(e)
      case e: IOException =>
        close()
        Failure(e)
    }

  def close(): Unit =
    channel.getAndUpdate(asJavaUnaryOperator { c =>
      if (c != null) c.close()
      null
    })
}

