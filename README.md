# fluflu
fluent logger for scala

[![wercker status](https://app.wercker.com/status/d754e7976e64af6e1065568b43b27ac7/m "wercker status")](https://app.wercker.com/project/bykey/d754e7976e64af6e1065568b43b27ac7)

[![codecov.io](http://codecov.io/github/tkrs/fluflu/coverage.svg?branch=master)](http://codecov.io/github/tkrs/fluflu?branch=master)

## Usage

Add to your `build.sbt`

```scala
libraryDependencies += "com.github.tkrs" %% "fluflu-queue" % "0.5.4"
```

## Example
```scala
import java.time.{ Clock, Duration }

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

object Main extends App {

  case class CCC(
    i: Int,
    ttt: String,
    uuu: String,
    sss: Int, mmm: Map[String, String],
    ggg: Seq[Double]
  )

  implicit val clock: Clock = Clock.systemUTC()

  val rnd: Random = new Random(System.nanoTime())
  val reconnectionBackoff: Backoff =
    ExponentialBackoff(Duration.ofNanos(500), Duration.ofSeconds(5), rnd)
  val rewriteBackoff: Backoff =
    ExponentialBackoff(Duration.ofNanos(500), Duration.ofSeconds(5), rnd)

  val messenger = fluflu.DefaultMessenger(
    host = "127.0.0.1",
    port = 24224,
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
  println(r)

  writer.close()
}
```

## TODO

- TEST

## LICENSE

MIT
