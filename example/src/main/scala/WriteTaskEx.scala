import fluflu._, data._
import io.circe.generic.auto._

import scala.concurrent.Await
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._

object WriteTaskEx extends App {

  // implicit val sender = DefaultSender(host = "192.168.99.100")
  implicit val sender = DefaultSender(host = "127.0.0.1")

  val wt = WriteTask()

  case class CCC(
    ttt: String,
    uuu: String,
    sss: Int, mmm: Map[String, String], ggg: Seq[Double]
  )

  val ccc = CCC("foo", "", Int.MaxValue, Map("name" -> "fluflu"), Seq(1.2, Double.MaxValue, Double.MinValue))

  Await.result(wt(Event("debug", "ccc", ccc)), 1000 millis)

  case class BBB(
    zzz: Long,
    yyy: Long,
    iii: Int,
    jjj: Int,
    list: CCC
  )

  val bbb = BBB(Long.MaxValue, Long.MinValue, Int.MinValue, Int.MaxValue, ccc)

  Await.result(wt(Event("debug", "bbb", bbb)), 1000 millis)

}
