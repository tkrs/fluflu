package fluflu.msgpack.shapes.ast

import java.{util => ju}

sealed trait MsgPack { self =>
  def add(k: MsgPack, v: MsgPack): MsgPack = self
}
object MsgPack {
  final case object MEmpty                                          extends MsgPack
  final case object MNil                                            extends MsgPack
  final case class MBool(a: Boolean)                                extends MsgPack
  final case class MString(a: String)                               extends MsgPack
  final case class MByte(a: Byte)                                   extends MsgPack
  final case class MExtension(typ: Byte, size: Int, a: Array[Byte]) extends MsgPack
  final case class MShort(a: Short)                                 extends MsgPack
  final case class MInt(a: Int)                                     extends MsgPack
  final case class MLong(a: Long)                                   extends MsgPack
  final case class MBigInt(a: BigInt)                               extends MsgPack
  final case class MBigDecimal(a: BigDecimal)                       extends MsgPack
  final case class MDouble(a: Double)                               extends MsgPack
  final case class MFloat(a: Float)                                 extends MsgPack
  final case class MArray(a: Vector[MsgPack])                       extends MsgPack
  final case class MMap(a: MutMap) extends MsgPack {
    override def add(k: MsgPack, v: MsgPack): MsgPack = {
      this.copy(a.add(k, v))
    }
  }
}

private[shapes] final class MutMap { self =>
  import scala.collection.JavaConverters._

  private[this] val _map  = new ju.HashMap[MsgPack, MsgPack]
  private[this] val _keys = Vector.newBuilder[MsgPack]

  def add(k: MsgPack, v: MsgPack): MutMap = {
    if (_map.containsKey(k)) self
    else {
      _map.put(k, v)
      _keys += k
      self
    }
  }

  def keys: Seq[MsgPack] = _keys.result().reverse

  def size: Int = _map.size

  def get(key: MsgPack): Option[MsgPack] = Option(_map.get(key))

  def iterator: Iterator[(MsgPack, MsgPack)] = new Iterator[(MsgPack, MsgPack)] {

    private[this] val keys = self.keys.iterator

    override def hasNext: Boolean = keys.hasNext

    override def next(): (MsgPack, MsgPack) = {
      val key = keys.next()
      key -> self._map.get(key)
    }
  }

  override def toString: String =
    s"CMap(${keys.zip(_map.values().asScala.mkString("[", ", ", "]"))})"
}

object MutMap {
  def empty: MutMap = new MutMap
}
