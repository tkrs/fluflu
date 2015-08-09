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
  )(implicit strategy: Strategy, f: Event[A] => Json) =
    new WriteActor[A](tagPrefix, host, port, timeout, bufferCapacity)
}

class WriteActor[A](
    val tagPrefix: String,
    val host: String,
    val port: Int,
    val timeout: Int,
    val bufferCapacity: Int
)(implicit strategy: Strategy, f: Event[A] => Json) {

  import Actor._

  val name = s"${host}_${port}_${timeout}_${bufferCapacity}"

  private[this] val server = new InetSocketAddress(host, port)
  private[this] val channel = SocketChannel.open(server)
  channel.socket().setSoTimeout(timeout)

  private[this] val act: Actor[Event[A]] =
    actor(
      { msg =>
        val b = buf(msg)
        channel.write(b)
      }, { e: Throwable =>
        println(e)
      }
    )
  def !(evt: Event[A]) = act ! evt

  private[this] def buf(evt: Event[A])(implicit f: Event[A] => Json): Array[ByteBuffer] = {
    // val event = Json("tag" := jString(s"${tag}.${evt.tag}")) -->>: Json("time" := jNumber(evt.time)) -->>: f(evt.record)
    val event = f(evt)
    val instance = ArgonautMsgpack.jsonCodec(
      ArgonautUnpackOptions.default
    )
    val pack = instance.toBytes(event, MsgOutBuffer.create())
    pack.grouped(bufferCapacity).map(ByteBuffer.wrap(_)).toArray
  }

  override def toString = name

}
