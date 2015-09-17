import fluflu.{ DefaultSender, WriteTask }
import fluflu.data.Event

import io.circe.generic.auto._

import scalaz.{ \/-, -\/ }

object WriteTaskEx extends App {

  implicit val sender = DefaultSender()

  val wt = WriteTask()

  case class CCC(
    ttt: String,
    sss: Int, mmm: Map[String, String], ggg: Seq[Double]
  )

  val ccc = CCC("foo", Int.MaxValue, Map("name" -> "fluflu"), Seq(1.2, Double.MaxValue, Double.MinValue))

  wt(Event("aaa", "ccc", ccc)).attemptRun match {
    case -\/(e) => e.printStackTrace()
    case \/-(o) => println(o)
  }

  case class BBB(
    zzz: Long,
    yyy: Long,
    iii: Int,
    jjj: Int,
    list: CCC
  )

  val bbb = BBB(Long.MaxValue, Long.MinValue, Int.MinValue, Int.MaxValue, ccc)

  wt(Event("aaa", "bbb", bbb)).attemptRun match {
    case -\/(e) => e.printStackTrace()
    case \/-(o) => println(o)
  }
}
