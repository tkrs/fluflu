package fluflu

import cats.data.Xor
import fluflu.msgpack._
import io.circe.Json
import io.circe.parser._
import io.circe.generic.auto._
import org.scalatest._

class JsonPackerSpec extends FlatSpec with Matchers {

  def bytesToLong(buf: Array[Byte]): Long =
    buf.foldRight((0, 0L))((l, r) => r match {
      case (i, n) => (i + 8, ((l & 0xff).toLong << i) | n)
    })._2

  val instance = MessagePack getInstance JSON

  it should "pack to long" in {
    val x = instance pack Json.long(Long.MaxValue)

    x match {
      case Xor.Right(v) => assert(bytesToLong(v) == Long.MaxValue)
      case Xor.Left(e) => fail()
    }

    case class Employee(name: String = "")

    val js = """{"name": "Shun Yanaura"}"""

    val emp = parse(js).getOrElse(Json.empty)

    val y = instance pack emp
    y match {
      case Xor.Left(e) => //
      case Xor.Right(v) =>
        println(s"size[${v.length}] => ${v.map(b => "%02X".format(b)).mkString.toLowerCase}")
    }
  }

}
