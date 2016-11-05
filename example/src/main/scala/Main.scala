import java.time.{ Clock, Duration }

import cats.data.Xor
import fluflu._
import io.circe.generic.auto._

import scala.concurrent.duration.Duration._
import scala.concurrent.{ Await, Future }
import scala.util.Random

object Main extends App {

  implicit val clock = Clock.systemUTC()

  val reconnectionBackoff = ExponentialBackoff(Duration.ofNanos(500000000L), Duration.ofSeconds(5), new Random(System.nanoTime()))
  val rewriteBackoff = ExponentialBackoff(Duration.ofNanos(500000000L), Duration.ofSeconds(5), new Random(System.nanoTime()))

  val wt = Writer(
    host = args(0),
    port = args(1).toInt,
    reconnectionTimeout = Duration.ofSeconds(10),
    rewriteTimeout = Duration.ofSeconds(10),
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

  val xs = Stream.from(1).map(x => Event("docker", "ccc", ccc.copy(i = x))).take(5000)

  import scala.concurrent.ExecutionContext.Implicits.global

  import cats.implicits._
  println("start")
  val rt: Future[Stream[Throwable Xor Unit]] = xs.traverse(a => if (!wt.die) wt.writeFuture(a) else Future.failed(new Exception("die")))
  Await.result(rt, Inf)
  println("done")
  wt.close()
}
