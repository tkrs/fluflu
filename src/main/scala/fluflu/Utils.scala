package fluflu

import java.nio.ByteBuffer

import argonaut._, Argonaut._
import msgpack4z._
import scalaz.concurrent._

object Utils {
  private[fluflu] def createBuffer[A](tagPrefix: String, bufferCapacity: Int, evt: Event[A])(implicit f: RecordDecoder[A]): Array[ByteBuffer] = {
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
}
