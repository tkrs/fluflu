import java.util.concurrent.TimeUnit

import cats.data.Xor
import fluflu._
import data._
import io.circe.generic.auto._

import scala.concurrent.duration.Duration
import scala.concurrent.{ Await, Future }
import scala.util.Random

object Main extends App {

  val reconnectionBackoff = ExponentialBackoff(500000000L, new Random(System.nanoTime()))
  val rewriteBackoff = ExponentialBackoff(500000000L, new Random(System.nanoTime()))

  val wt = Writer(
    host = "192.168.99.100",
    port = 24224,
    timeout = 3000,
    clientPoolSize = 1,
    messengerPoolSize = 2,
    maxConnectionRetries = 8,
    maxWriteRetries = 8,
    reconnectionBackoff = reconnectionBackoff,
    rewriteBackoff = rewriteBackoff
  )

  case class CCC(
    i: Int,
    ttt: String,
    uuu: String,
    sss: Int, mmm: Map[String, String],
    ggg: Seq[Double]
  )

  val ccc = CCC(0, "foo", "", Int.MaxValue, Map("name" -> "fluflu"), Seq(1.2, Double.MaxValue, Double.MinValue))

  val st = Stream.from(1).map(x => Event("docker", "ccc", ccc.copy(i = x))).take(50000)

  import scala.concurrent.ExecutionContext.Implicits.global

  import cats.implicits._
  val rt: Future[Stream[Throwable Xor Unit]] = st.traverse(a => if (!wt.die) wt.write(a) else Future.failed(new Exception("die")))

  Await.result(rt, Duration.Inf)

  TimeUnit.SECONDS.sleep(30)

  wt.close()

  println("done")
}
