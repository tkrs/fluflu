package fluflu

import java.io.IOException
import java.lang.{Boolean => JBool}
import java.net.{InetSocketAddress, StandardSocketOptions}
import java.nio.ByteBuffer
import java.nio.channels.SocketChannel
import java.time.{Clock, Duration}

import com.typesafe.scalalogging.LazyLogging

import scala.annotation.tailrec
import scala.util.{Failure, Success, Try}

trait Connection {
  def write(message: ByteBuffer): Try[Unit]
  def isClosed: Boolean
  def close(): Try[Unit]
}

object Connection {

  def apply(remote: InetSocketAddress, timeout: Duration, backoff: Backoff)(
      implicit clock: Clock = Clock.systemUTC()): Connection =
    new ConnectionImpl(remote, timeout, backoff)

  class ConnectionImpl(remote: InetSocketAddress, timeout: Duration, backoff: Backoff)(
      implicit clock: Clock)
      extends Connection
      with LazyLogging {
    import StandardSocketOptions._

    @volatile private[this] var closed: Boolean = false

    @volatile private[this] var channel: SocketChannel =
      doConnect(channelOpen, 0, Sleeper(backoff, timeout, clock)).get

    protected def channelOpen: SocketChannel = {
      val ch = SocketChannel.open()
      ch.setOption[JBool](TCP_NODELAY, true)
      ch.setOption[JBool](SO_KEEPALIVE, true)
      ch
    }

    @tailrec private def doConnect(ch: SocketChannel,
                                   retries: Int,
                                   sleeper: Sleeper): Try[SocketChannel] = {
      logger.debug(s"Start connecting to $remote. retries: $retries")
      try {
        if (ch.connect(remote)) Success(ch)
        else Failure(new IOException(s"Failed to connect: $remote"))
      } catch {
        case e: IOException =>
          if (sleeper.giveUp) {
            closed = true
            if (ch.isOpen) ch.close()
            Failure(e)
          } else {
            sleeper.sleep(retries)
            ch.close()
            doConnect(channelOpen, retries + 1, sleeper)
          }
      }
    }

    @throws[Exception]("If the connection was already closed")
    @throws[IOException]
    def connect(): Try[SocketChannel] =
      if (closed) Failure(new Exception("Already closed"))
      else if (channel.isConnected) Success(channel)
      else
        doConnect(channelOpen, 0, Sleeper(backoff, timeout, clock)) match {
          case t @ Success(c) => channel = c; t
          case f              => f
        }

    def isClosed: Boolean =
      closed || channel.isConnected

    def write(message: ByteBuffer): Try[Unit] =
      for {
        ch <- connect()
        _ <- Try {
              logger.trace(s"Start writing message: $message")
              @tailrec def go(acc: Int): Int =
                if (!message.hasRemaining) acc
                else go(acc + ch.write(message))
              val toWrite = go(0)
              logger.trace(s"Number of bytes written: $toWrite")
            }
      } yield ()

    def close(): Try[Unit] = {
      closed = true
      logger.debug("Start closing connection.")
      Try(channel.close())
    }
  }
}
