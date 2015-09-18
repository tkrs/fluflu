package fluflu.msgpack

import fluflu.msgpack.json.MessagePackJson

trait Msgpack[A] {
  def pack(a: A): Seq[Byte]
  def unpack(a: Seq[Byte]): Option[A]
}

object Msgpack {
  def getInstance(i: Instances) = i match {
    case JSON => MessagePackJson()
    case MAP => ???
  }
}

sealed abstract class Instances(t: String)
object Instances {
  def of(t: String) = t match {
    case "JSON" => JSON
    case "MAP" => MAP
    case _ => ???
  }
}
case object JSON extends Instances("JSON")
case object MAP extends Instances("MAP")

