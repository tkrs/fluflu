import java.time.{ Clock, Duration }
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.{ Executors, TimeUnit }

import com.typesafe.scalalogging.LazyLogging
import fluflu._
import fluflu.queue.Async
import io.circe.generic.auto._

import scala.util.Random

case class CCC(
  i: Int,
  aaa: String,
  bbb: String,
  ccc: String,
  ddd: Int,
  eee: Map[String, String],
  ffff: Seq[Double],
  ggg: Int,
  hhh: String,
  iii: Int,
  jjj: String,
  kkk: String,
  lll: String,
  mmm: String,
  nnn: String,
  ooo: String,
  ppp: String,
  qqq: Int,
  rrr: Int,
  sss: Int,
  ttt: Int,
  uuu: Int,
  vvv: Int,
  www: Int,
  xxx: Int,
  yyy: Int,
  zzz: Int
)

/**
 * sbt "examples/runMain Main 127.0.0.1 24224 10 100"
 */
object Main extends App with LazyLogging {

  val rnd: Random = new Random(System.nanoTime())

  val ccc: CCC = CCC(
    0,
    rnd.nextString(10),
    rnd.nextString(10),
    rnd.nextString(10),
    rnd.nextInt(Int.MaxValue),
    Map("name" -> "fluflu"),
    Seq(1.2, Double.MaxValue, Double.MinValue),
    rnd.nextInt(Int.MaxValue),
    rnd.nextString(10),
    rnd.nextInt(Int.MaxValue),
    rnd.nextString(10),
    rnd.nextString(10),
    rnd.nextString(10),
    rnd.nextString(10),
    rnd.nextString(10),
    rnd.nextString(10),
    rnd.nextString(10),
    rnd.nextInt(Int.MaxValue),
    rnd.nextInt(Int.MaxValue),
    rnd.nextInt(Int.MaxValue),
    rnd.nextInt(Int.MaxValue),
    rnd.nextInt(Int.MaxValue),
    rnd.nextInt(Int.MaxValue),
    rnd.nextInt(Int.MaxValue),
    rnd.nextInt(Int.MaxValue),
    rnd.nextInt(Int.MaxValue),
    rnd.nextInt(Int.MaxValue)
  )

  println(ccc)

  implicit val clock: Clock = Clock.systemUTC()

  val reconnectionBackoff: Backoff =
    ExponentialBackoff(Duration.ofNanos(500), Duration.ofSeconds(5), rnd)
  val rewriteBackoff: Backoff =
    ExponentialBackoff(Duration.ofNanos(500), Duration.ofSeconds(5), rnd)

  val messenger = DefaultMessenger(
    host = args(0),
    port = args(1).toInt,
    reconnectionTimeout = Duration.ofSeconds(10),
    rewriteTimeout = Duration.ofSeconds(10),
    reconnectionBackoff = reconnectionBackoff,
    rewriteBackoff = rewriteBackoff
  )
  val asyncQueue: Async = Async(
    messenger = messenger,
    initialDelay = Duration.ofMillis(50),
    delay = Duration.ofMillis(100),
    terminationDelay = Duration.ofSeconds(10)
  )
  val push: Event[CCC] => Unit = { a =>
    asyncQueue.push(a)
  }

  val idx = new AtomicInteger(0)
  val xs: Iterator[Event[CCC]] =
    Iterator.from(1)
      .map(i => Event("example", "ccc", ccc.copy(i = i)))
      .take(args(3).toInt)

  val runner = Executors.newSingleThreadExecutor()

  logger.info(s"Start")
  val start = System.nanoTime()
  runner.execute(() => xs foreach push)

  runner.awaitTermination(args(2).toLong, TimeUnit.SECONDS)
  runner.shutdownNow()
  asyncQueue.close()

  logger.info(s"A queue remaining: ${asyncQueue.remaining}")
  logger.info(s"Elapsed time: ${TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - start)} ms")
}
