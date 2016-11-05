package fluflu

import java.io.IOException
import java.lang.{ Boolean => JBool }
import java.net.{ InetSocketAddress, StandardSocketOptions }
import java.nio.ByteBuffer
import java.nio.channels.{ NotYetConnectedException, SocketChannel }
import java.time.{ Clock, Duration, Instant }
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference

import scala.annotation.tailrec
import scala.compat.java8.FunctionConverters._
import scala.concurrent.blocking
import scala.util.{ Failure, Try }

final case class Connection(
    remote: InetSocketAddress,
    connectionRetryTimeout: Duration,
    backoff: Backoff
)(implicit clock: Clock) {
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
      val x = if (c == null) { open } else c
      @tailrec def doConnect(retries: Int, start: Instant): SocketChannel = {
        try {
          if (x.connect(remote)) x else throw new Exception()
        } catch {
          case _: IOException =>
            if (Instant.now(clock).minusNanos(connectionRetryTimeout.toNanos).compareTo(start) <= 0) {
              blocking {
                TimeUnit.NANOSECONDS.sleep(backoff.nextDelay(retries).toNanos)
              }
              x.close()
              doConnect(retries + 1, start)
            } else {
              if (x.isOpen) x.close()
              noLongerRetriable = true
              null
            }
        }
      }
      doConnect(0, Instant.now(clock))
    })

  connect()

  def isClosed: Boolean = channel.get == null

  def write(message: ByteBuffer): Try[Int] =
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

