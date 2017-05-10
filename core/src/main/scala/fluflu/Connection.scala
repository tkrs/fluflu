package fluflu

import java.io.IOException
import java.lang.{ Boolean => JBool }
import java.net.{ InetSocketAddress, StandardSocketOptions }
import java.nio.ByteBuffer
import java.nio.channels.SocketChannel
import java.time.{ Clock, Duration }
import java.util.concurrent.atomic.AtomicReference

import cats.syntax.option._
import cats.syntax.either._
import monix.eval.Task

import scala.annotation.tailrec
import scala.compat.java8.FunctionConverters._
import scala.util.control.NonFatal

trait Connection {
  def write(message: ByteBuffer): Task[Unit]
  def isClosed: Boolean
  def close(): Unit
}

object Connection {
  def apply(
    remote: InetSocketAddress,
    timeout: Duration,
    backoff: Backoff
  )(implicit clock: Clock = Clock.systemUTC()): Connection =
    new ConnectionImpl(remote, timeout, backoff)

  final class ConnectionImpl(
      remote: InetSocketAddress,
      timeout: Duration,
      backoff: Backoff
  )(implicit clock: Clock) extends Connection {
    import StandardSocketOptions._

    private[this] val channel: AtomicReference[Either[Throwable, Option[SocketChannel]]] =
      new AtomicReference(go(open, 0, Sleeper(backoff, timeout, clock)))

    private[this] def open = {
      val ch = SocketChannel.open()
      ch.setOption[JBool](TCP_NODELAY, true)
      ch.setOption[JBool](SO_KEEPALIVE, true)
      ch
    }

    @tailrec private def go(x: SocketChannel, retries: Int, sleeper: Sleeper): Either[Throwable, Option[SocketChannel]] = {
      try
        if (x.connect(remote)) x.some.asRight
        else new IOException("Failed to connect").asLeft
      catch {
        case e: IOException =>
          if (sleeper.giveUp) {
            if (x.isOpen) x.close()
            close()
            e.asLeft
          } else {
            sleeper.sleep(retries)
            x.close()
            go(open, retries + 1, sleeper)
          }
      }
    }

    def connect(): Task[Option[SocketChannel]] = {
      val c = channel.get() match {
        case t @ Right(None) =>
          t
        case t @ Right(Some(ch)) if ch.isConnected =>
          t
        case _ =>
          go(open, 0, Sleeper(backoff, timeout, clock))
      }
      Task.fromTry(c.toTry)
    }

    def isClosed: Boolean = channel.get.fold(
      _ => true,
      _.fold(false)(!_.isConnected)
    )

    def write(message: ByteBuffer): Task[Unit] =
      connect().map(_.map { ch =>
        try ch.write(message) catch {
          case ie: IOException =>
            ch.close()
            throw ie
        }
      })

    def close(): Unit =
      channel.updateAndGet(asJavaUnaryOperator {
        case Left(e) => none.asRight
        case Right(ch) =>
          try {
            ch.foreach(_.close()); none.asRight
          } catch {
            case NonFatal(e) => none.asRight
          }
      })
  }
}
