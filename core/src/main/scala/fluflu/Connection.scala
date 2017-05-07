package fluflu

import java.io.IOException
import java.lang.{ Boolean => JBool }
import java.net.{ InetSocketAddress, StandardSocketOptions }
import java.nio.ByteBuffer
import java.nio.channels.{ NotYetConnectedException, SocketChannel }
import java.time.{ Clock, Duration }
import java.util.concurrent.atomic.AtomicReference

import cats.instances.either._
import cats.syntax.option._
import cats.syntax.either._
import cats.syntax.flatMap._
import scala.annotation.tailrec
import scala.compat.java8.FunctionConverters._

trait Connection {
  def write(message: ByteBuffer): Either[Throwable, Unit]
  def isClosed: Boolean
  def close(): Unit
}

object Connection {

  def apply(
    remote: InetSocketAddress,
    timeout: Duration,
    backoff: Backoff
  )(implicit clock: Clock): Connection =
    new SyncConnection(remote, timeout, backoff, clock)

  private[this] final class SyncConnection(
      remote: InetSocketAddress,
      timeout: Duration,
      backoff: Backoff,
      clock: Clock
  ) extends Connection {
    import StandardSocketOptions._

    private[this] val channel: AtomicReference[Either[Throwable, Option[SocketChannel]]] =
      new AtomicReference(go(open, Sleeper(backoff, timeout, clock)))

    private[this] def open = {
      val ch = SocketChannel.open()
      ch.setOption[JBool](TCP_NODELAY, true)
      ch
    }

    @tailrec private[this] def go(x: SocketChannel, sleeper: Sleeper): Either[Throwable, Option[SocketChannel]] = {
      try {
        if (x.connect(remote)) Either.right(x.some) else Either.left(new IOException("Failed to connect"))
      } catch {
        case e: IOException =>
          if (sleeper.giveup) {
            if (x.isOpen) x.close()
            Either.left(e)
          } else {
            sleeper.sleep()
            x.close()
            go(open, sleeper)
          }
      }
    }

    def connect(): Either[Throwable, Option[SocketChannel]] =
      channel.updateAndGet(asJavaUnaryOperator {
        case t @ Right(Some(ch)) if ch.isConnected || ch.isConnectionPending => t
        case _ => go(open, Sleeper(backoff, timeout, clock))
      })

    def isClosed: Boolean = channel.get.fold(
      Function.const(false),
      _.fold(false)(ch => !(ch.isConnected || ch.isConnectionPending))
    )

    def write(message: ByteBuffer): Either[Throwable, Unit] = {
      val r = connect() >>= (ch => Either.catchNonFatal(ch.map(_.write(message))))
      r.fold({
        case e: NotYetConnectedException =>
          channel.get >>= {
            case Some(ch) => if (ch.finishConnect()) Either.right(()) else Either.left(e)
            case None => Either.right(())
          }
        case e: IOException =>
          close(); Either.left(e)
        case e: Throwable =>
          Either.left(e)
      }, _ => Either.right(()))
    }

    def close(): Unit =
      channel.updateAndGet(asJavaUnaryOperator {
        case Right(ch) => Either.catchNonFatal { ch.foreach(_.close()); none }
        case _ => Either.right(none)
      })
  }
}
