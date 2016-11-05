import java.time.Duration
import java.util.concurrent.TimeUnit

import cats.data.Xor
import fluflu._
import data._
import io.circe.generic.auto._

import scala.concurrent.duration.Duration._
import scala.concurrent.{ Await, Future }
import scala.util.Random

object Main extends App {

  val reconnectionBackoff = ExponentialBackoff(Duration.ofNanos(500000000L), Duration.ofSeconds(5), new Random(System.nanoTime()))
  val rewriteBackoff = ExponentialBackoff(Duration.ofNanos(500000000L), Duration.ofSeconds(5), new Random(System.nanoTime()))

  val wt = Writer(
    host = args(0),
    port = args(1).toInt,
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

  val st = Stream.from(1).map(x => Event("docker", "ccc", ccc.copy(i = x))).take(2000)

  import scala.concurrent.ExecutionContext.Implicits.global

  import cats.implicits._
  val rt: Future[Stream[Throwable Xor Unit]] = st.traverse(a => if (!wt.die) wt.writeFuture(a) else Future.failed(new Exception("die")))

  TimeUnit.SECONDS.sleep(1)

  Await.result(rt, Inf)

  wt.close()

  println("done")
}
