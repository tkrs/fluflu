package fluflu

import java.net.{ InetSocketAddress, StandardSocketOptions }
import java.nio.ByteBuffer
import java.nio.channels.SocketChannel

import argonaut._, Argonaut._
import msgpack4z._
import scalaz.concurrent._
import scala.util.Try

object WriteActor {
  def apply[A](
    tagPrefix: String,
    host: String = "localhost",
    port: Int = 24224,
    timeout: Int = 3 * 1000,
    bufferCapacity: Int = 1 * 1024 * 1024
  )(implicit strategy: Strategy, f: A => Json) =
    new WriteActor[A](tagPrefix, host, port, timeout, bufferCapacity)
}

class WriteActor[A](
    val tagPrefix: String,
    val host: String,
    val port: Int,
    val timeout: Int,
    val bufferCapacity: Int
)(implicit strategy: Strategy, f: A => Json) {

  import Actor._

  val name = s"${host}_${port}_${timeout}_${bufferCapacity}"

  var channel = SocketChannel.open()

  private[this] val server = new InetSocketAddress(host, port)

  def connect(ch: SocketChannel, remote: InetSocketAddress): Boolean = {

    val tryChannel = Try {
      ch.socket().setSoTimeout(timeout)
      ch.setOption[java.lang.Boolean](StandardSocketOptions.SO_REUSEADDR, true);
      ch.setOption[java.lang.Boolean](StandardSocketOptions.SO_KEEPALIVE, true);
      ch.connect(remote)
    } recover {
      case e: Throwable =>
        e.printStackTrace()
        false
      case e => false
    }

    tryChannel.getOrElse(false)

  }

  connect(channel, server)

  private[this] val act: Actor[Event[A]] =
    actor(
      { msg =>
        channel.isConnected || {
          channel = SocketChannel.open()
          connect(channel, server)
        }
        channel.write(createBuffer(msg))
      }, { e: Throwable =>
        if (channel.isConnected) channel.close()
        e.printStackTrace()
        throw e
      }
    )
  def !(evt: Event[A]) = act ! evt

  private[this] def createBuffer(evt: Event[A])(implicit f: A => Json): Array[ByteBuffer] = {
    val tag = jString(s"${tagPrefix}.${evt.label}")
    val time = jNumber(evt.time)
    val record = f(evt.record)
    val event = jArrayElements(tag, time, record)
    val instance = ArgonautMsgpack.jsonCodec(
      ArgonautUnpackOptions.default
    )
    val pack = instance.toBytes(event, MsgOutBuffer.create())
    pack.grouped(bufferCapacity).map(ByteBuffer.wrap(_)).toArray
  }

  override def toString = name

  override def finalize(): Unit = {
    super.finalize()
    channel.close()
  }

}
