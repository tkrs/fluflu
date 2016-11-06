import java.time.{ Clock, Duration }

import cats.MonadError
import cats.data.Xor
import cats.instances.future._
import cats.instances.stream._
import cats.syntax.traverse._
import fluflu._
import io.circe.generic.auto._

import scala.concurrent.duration.Duration._
import scala.concurrent.{ Await, Future }
import scala.concurrent.ExecutionContext.Implicits.global
import scala.util.Random

/**
  * sbt "examples/runMain Main 127.0.0.1 24224"
  */
object Main extends App {

  case class CCC(
    i: Int,
    ttt: String,
    uuu: String,
    sss: Int,
    mmm: Map[String, String],
    ggg: Seq[Double]
  )

  implicit val clock: Clock = Clock.systemUTC()

  val rnd: Random = new Random(System.nanoTime())
  val reconnectionBackoff: Backoff =
    ExponentialBackoff(Duration.ofNanos(500), Duration.ofSeconds(5), rnd)
  val rewriteBackoff: Backoff =
    ExponentialBackoff(Duration.ofNanos(500), Duration.ofSeconds(5), rnd)

  val messenger = fluflu.DefaultMessenger(
    host = args(0),
    port = args(1).toInt,
    reconnectionTimeout = Duration.ofSeconds(10),
    rewriteTimeout = Duration.ofSeconds(10),
    reconnectionBackoff = reconnectionBackoff,
    rewriteBackoff = rewriteBackoff
  )

  val writer: Writer = Writer(messenger)

  val ccc: CCC = CCC(0, "foo", "", Int.MaxValue, Map("name" -> "fluflu"), Seq(1.2, Double.MaxValue, Double.MinValue))

  val xs: Stream[Event[CCC]] =
    Stream.from(1).map(x => Event("example", "ccc", ccc.copy(i = x))).take(5000)

  val write: Event[CCC] => Future[Unit] = { a =>
    if (writer.die) Future.failed(new Exception("die"))
    else writer.writeFuture(a).flatMap(_.fold(Future.failed, Future.successful))
  }

  val f: Future[Stream[Unit]] = xs.traverse(write)
  val fa: Future[Xor[Throwable, Stream[Unit]]] = MonadError[Future, Throwable].attempt(f)
  val r: Xor[Throwable, Stream[Unit]] = Await.result(fa, Inf)
  println(r.isRight)

  writer.close()
}
