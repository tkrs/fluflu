package fluflu

import java.nio.ByteBuffer

import argonaut._, Argonaut._
import msgpack4z._
import scalaz.concurrent._

object WriteActor {
  def apply[A](
    tagPrefix: String,
    bufferCapacity: Int = 1 * 1024 * 1024
  )(implicit sender: Sender, strategy: Strategy, decoder: RecordDecoder[A], onError: Throwable => Unit) =
    new WriteActor[A](tagPrefix, bufferCapacity)

  implicit def onErrorDefault(e: Throwable): Unit = {
    e.printStackTrace()
  }
}

class WriteActor[A](
    val tagPrefix: String,
    val bufferCapacity: Int
)(implicit sender: Sender, strategy: Strategy, decoder: RecordDecoder[A], onError: Throwable => Unit) {

  import Actor._

  private[this] def createBuffer(evt: Event[A])(implicit f: RecordDecoder[A]): Array[ByteBuffer] = {
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

  private[this] val act: Actor[Event[A]] =
    actor(
      { msg =>
        val buf = createBuffer(msg)
        sender.write(buf)
      }, { e: Throwable =>
        if (sender.isConnected) sender.close()
        onError(e)
      }
    )

  def apply(a: Event[A]): Unit = this ! a

  def contramap[B](f: B => Event[A]): Actor[B] = new Actor[B](b => this ! f(b), onError)(strategy)

  def !(evt: Event[A]) = act ! evt

  def close() = sender.close()

  override def finalize(): Unit = {
    super.finalize()
    close()
  }

}
