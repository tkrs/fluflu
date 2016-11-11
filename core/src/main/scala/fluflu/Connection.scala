package fluflu

import java.io.IOException
import java.lang.{ Boolean => JBool }
import java.net.{ InetSocketAddress, StandardSocketOptions }
import java.nio.ByteBuffer
import java.nio.channels.{ NotYetConnectedException, SocketChannel }
import java.time.{ Clock, Duration, Instant }
import java.util.concurrent.TimeUnit.NANOSECONDS
import java.util.concurrent.atomic.AtomicReference

import scala.annotation.tailrec
import scala.compat.java8.FunctionConverters._
import scala.concurrent.blocking
import scala.util.{ Failure, Success, Try }

final case class Connection(
    remote: InetSocketAddress,
    reconnectionTimeout: Duration,
    reconnectionBackoff: Backoff
)(implicit clock: Clock) {
  import StandardSocketOptions._

  private[this] val channel: AtomicReference[Try[SocketChannel]] =
    new AtomicReference(go(open, 0, Instant.now(clock)))

  private[this] def open = {
    val ch = SocketChannel.open()
    ch.setOption[JBool](TCP_NODELAY, true)
    ch
  }

  @tailrec private[this] def go(x: SocketChannel, retries: Int, start: Instant): Try[SocketChannel] = {
    try {
      if (x.connect(remote)) Success(x) else throw new IOException()
    } catch {
      case e: IOException =>
        if (Instant.now(clock).minusNanos(reconnectionTimeout.toNanos).compareTo(start) <= 0) {
          blocking(NANOSECONDS.sleep(reconnectionBackoff.nextDelay(retries).toNanos))
          x.close()
          go(open, retries + 1, start)
        } else {
          if (x.isOpen) x.close()
          Failure(e)
        }
    }
  }

  def connect(): Try[SocketChannel] =
    channel.updateAndGet(asJavaUnaryOperator {
      case t @ Success(ch) if ch.isConnected || ch.isConnectionPending => t
      case _ => go(open, 0, Instant.now(clock))
    })

  def isClosed: Boolean = channel.get match {
    case Success(ch) => !(ch.isConnected || ch.isConnectionPending)
    case Failure(_) => true
  }

  def write(message: ByteBuffer): Try[Unit] =
    connect().map { ch => ch.write(message); () } recoverWith {
      case e: NotYetConnectedException =>
        channel.get.flatMap { ch =>
          if (ch.finishConnect()) Success(())
          else Failure(new IOException("Failed to finish connect"))
        }
      case e: IOException =>
        close(); Failure(e)
    }

  def close(): Unit =
    channel.updateAndGet(asJavaUnaryOperator {
      case Success(ch) =>
        ch.close(); Failure(new Exception("Already closed"))
      case _ => Failure(new Exception("Already closed"))
    })
}

