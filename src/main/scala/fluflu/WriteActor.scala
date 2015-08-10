package fluflu

import java.net.InetSocketAddress
import java.nio.ByteBuffer
import java.nio.channels.SocketChannel

import argonaut._, Argonaut._
import msgpack4z._
import scalaz.concurrent._

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

  private[this] val server = new InetSocketAddress(host, port)
  val channel = SocketChannel.open(server)
  channel.socket().setSoTimeout(timeout)

  private[this] val act: Actor[Event[A]] =
    actor(
      { msg =>
        if (!channel.isConnected) channel.connect(server)
        channel.write(createBuffer(msg))
      }, { e: Throwable =>
        e.printStackTrace()
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

  override def finalize() = channel.close()

}
