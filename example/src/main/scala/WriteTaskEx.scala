import fluflu._
import data._
import io.circe.generic.auto._

import scala.concurrent.{ Await, Future }
import scala.concurrent.duration._

object WriteTaskEx extends App {

  import scala.concurrent.ExecutionContext.Implicits.global

  // implicit val sender = DefaultSender(host = "192.168.99.100")
  implicit val sender = DefaultSender(host = "127.0.0.1")

  val wt = WriteTask()

  case class CCC(
    i: Int,
    ttt: String,
    uuu: String,
    sss: Int, mmm: Map[String, String], ggg: Seq[Double]
  )

  val ccc = CCC(0, "foo", "", Int.MaxValue, Map("name" -> "fluflu"), Seq(1.2, Double.MaxValue, Double.MinValue))

  val st = (0 to 100000).toStream.map(x => Event("debug", "ccc", ccc.copy(i = x)))

  import cats.std.future._
  import cats.std.stream._
  import cats.syntax.traverse._

  val s = System.currentTimeMillis()
  val fs: Future[Stream[Unit]] = st.traverse { a => wt(a).map(_ => ()).recover { case e => println(e) } }

  val xs = Await.result(fs, Duration.Inf)
  println(System.currentTimeMillis() - s)

  case class BBB(
    zzz: Long,
    yyy: Long,
    iii: Int,
    jjj: Int,
    list: CCC
  )

  val bbb = BBB(Long.MaxValue, Long.MinValue, Int.MinValue, Int.MaxValue, ccc)

  val f = wt(Event("debug", "bbb", bbb))

  Await.result(f, 1000 millis)

  wt.close()

}
