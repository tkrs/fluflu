package fluflu

import java.io.IOException
import java.lang.{Boolean => JBool}
import java.net.{SocketAddress, NetworkInterface, StandardSocketOptions}
import java.nio.ByteBuffer
import java.nio.channels.SocketChannel
import java.time.{Clock, Duration}

import com.typesafe.scalalogging.LazyLogging

import scala.annotation.tailrec
import scala.util.{Failure, Success, Try}

trait Connection {
  def writeAndRead(message: ByteBuffer): Try[ByteBuffer]
  def isClosed: Boolean
  def close(): Try[Unit]
}

object Connection {

  final case class Settings(
      connectionTimeout: Duration,
      connectionBackof: Backoff,
      writeTimeout: Duration,
      writeBackof: Backoff,
      readTimeout: Duration,
      readBackof: Backoff,
      readSize: Int = 55 // When using UUID v4 to chunk
  )

  final case class SocketOptions(
      soBroadcast: Option[Boolean] = None,
      soKeepalive: Option[Boolean] = None,
      soSndbuf: Option[Int] = None,
      soRcvbuf: Option[Int] = None,
      soReuseAddr: Option[Boolean] = None,
      soLinger: Option[Int] = None,
      ipTos: Option[Int] = None,
      ipMulticastIf: Option[NetworkInterface] = None,
      ipMulticastTtl: Option[Int] = None,
      ipMulticastLoop: Option[Boolean] = None,
      tcpNoDelay: Option[Boolean] = Some(true),
      soTimeout: Option[Int] = Some(5000)
  )

  def apply(remote: SocketAddress, socketOptions: SocketOptions, settings: Settings, clock: Clock): Connection =
    new ConnectionImpl(remote, socketOptions, settings)(clock)

  def apply(remote: SocketAddress, settings: Settings)(implicit
                                                       clock: Clock = Clock.systemUTC()): Connection =
    new ConnectionImpl(remote, SocketOptions(), settings)(clock)

  class ConnectionImpl(remote: SocketAddress, socketOptions: SocketOptions, settings: Settings)(implicit clock: Clock)
      extends Connection
      with LazyLogging {
    import StandardSocketOptions._

    @volatile private[this] var closed: Boolean = false

    @volatile private[this] var channel: SocketChannel =
      doConnect(channelOpen, 0, Sleeper(settings.connectionBackof, settings.connectionTimeout, clock)).get

    protected def channelOpen: SocketChannel = {
      val ch = SocketChannel.open()
      socketOptions.ipMulticastIf.foreach(ch.setOption(IP_MULTICAST_IF, _))
      socketOptions.ipMulticastLoop.foreach(ch.setOption[JBool](IP_MULTICAST_LOOP, _))
      socketOptions.ipMulticastTtl.foreach(ch.setOption[Integer](IP_MULTICAST_TTL, _))
      socketOptions.ipTos.foreach(ch.setOption[Integer](IP_TOS, _))
      socketOptions.soBroadcast.foreach(ch.setOption[JBool](SO_BROADCAST, _))
      socketOptions.soKeepalive.foreach(ch.setOption[JBool](SO_KEEPALIVE, _))
      socketOptions.soLinger.foreach(ch.setOption[Integer](SO_LINGER, _))
      socketOptions.soSndbuf.foreach(ch.setOption[Integer](SO_SNDBUF, _))
      socketOptions.soRcvbuf.foreach(ch.setOption[Integer](SO_RCVBUF, _))
      socketOptions.soReuseAddr.foreach(ch.setOption[JBool](SO_REUSEADDR, _))
      socketOptions.tcpNoDelay.foreach(ch.setOption[JBool](TCP_NODELAY, _))

      val s = ch.socket()
      socketOptions.soTimeout.foreach(s.setSoTimeout)
      ch
    }

    @tailrec private def doConnect(ch: SocketChannel, retries: Int, sleeper: Sleeper): Try[SocketChannel] = {
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
        doConnect(channelOpen, 0, Sleeper(settings.connectionBackof, settings.connectionTimeout, clock)) match {
          case t @ Success(c) => channel = c; t
          case f              => f
        }

    def isClosed: Boolean =
      closed || !channel.isConnected

    private def _write(message: ByteBuffer, ch: SocketChannel, retries: Int, sleeper: Sleeper): Try[Int] = {
      @tailrec def loop(acc: Int): Int =
        if (!message.hasRemaining) acc
        else loop(acc + ch.write(message))

      Try(loop(0)) match {
        case v @ Success(_) => v
        case Failure(e0) =>
          if (sleeper.giveUp) Failure(e0)
          else {
            sleeper.sleep(retries)
            channel.close()
            connect() match {
              case Success(ch1) => _write(message, ch1, retries + 1, sleeper)
              case Failure(e)   => Failure(e)
            }
          }
      }
    }

    private def _read(dst: ByteBuffer, size: Int, ch: SocketChannel, retries: Int, sleeper: Sleeper): Try[Int] = {
      Try(ch.read(dst)) match {
        case Success(sz) if size + sz == settings.readSize => Success(size + sz)
        case Success(sz)                                   => _read(dst, size + sz, ch, retries, sleeper)
        case Failure(e0) =>
          if (sleeper.giveUp) Failure(e0)
          else {
            sleeper.sleep(retries)
            channel.close()
            connect() match {
              case Success(ch1) => _read(dst, size, ch1, retries + 1, sleeper)
              case Failure(e)   => Failure(e)
            }
          }
      }
    }

    private[this] val ackBuffer = ByteBuffer.allocateDirect(256)

    def writeAndRead(message: ByteBuffer): Try[ByteBuffer] =
      for {
        ch <- connect()
        _  = logger.debug(s"Start writing message: $message")
        ws = Sleeper(settings.writeBackof, settings.writeTimeout, clock)
        toWrite <- _write(message, ch, 0, ws) if ch.isConnected
        _ = logger.debug(s"Number of bytes written: $toWrite")

        rs = Sleeper(settings.readBackof, settings.readTimeout, clock)
        _  = ackBuffer.clear()
        _ <- _read(ackBuffer, 0, ch, 0, rs) if ch.isConnected
        _ = logger.debug(s"Number of bytes read: ${ackBuffer.position()}")
      } yield {
        ackBuffer.flip()
        ackBuffer.duplicate()
      }

    def close(): Try[Unit] = {
      closed = true
      logger.debug("Start closing connection.")
      Try(channel.close())
    }
  }
}
