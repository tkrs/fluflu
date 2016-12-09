import java.time.{ Clock, Duration }
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.{ ExecutorService, Executors, ScheduledExecutorService, TimeUnit }

import cats.instances.future._
import cats.instances.vector._
import cats.syntax.traverse._
import cats.syntax.applicativeError._
import fluflu._
import fluflu.queue.Async
import io.circe.generic.auto._

import scala.concurrent.duration.Duration._
import scala.concurrent.{ Await, Future }
import scala.concurrent.ExecutionContext.Implicits.global
import scala.util.Random

/**
 * sbt "examples/runMain Main 127.0.0.1 24224 10"
 */
object Main extends App {

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

  val rnd0 = new Random()

  val ccc: CCC = CCC(
    0,
    rnd0.nextString(1000),
    rnd0.nextString(10),
    rnd0.nextString(100),
    rnd0.nextInt(Int.MaxValue),
    Map("name" -> "fluflu"),
    Seq(1.2, Double.MaxValue, Double.MinValue),
    rnd0.nextInt(Int.MaxValue),
    rnd0.nextString(30),
    rnd0.nextInt(Int.MaxValue),
    rnd0.nextString(30),
    rnd0.nextString(30),
    rnd0.nextString(30),
    rnd0.nextString(30),
    rnd0.nextString(30),
    rnd0.nextString(30),
    rnd0.nextString(30),
    rnd0.nextInt(Int.MaxValue),
    rnd0.nextInt(Int.MaxValue),
    rnd0.nextInt(Int.MaxValue),
    rnd0.nextInt(Int.MaxValue),
    rnd0.nextInt(Int.MaxValue),
    rnd0.nextInt(Int.MaxValue),
    rnd0.nextInt(Int.MaxValue),
    rnd0.nextInt(Int.MaxValue),
    rnd0.nextInt(Int.MaxValue),
    rnd0.nextInt(Int.MaxValue)
  )

  println(ccc)

  implicit val clock: Clock = Clock.systemUTC()

  val rnd: Random = new Random(System.nanoTime())
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
    initialBufferSize = 1024,
    initialDelay = 50,
    delay = 100,
    delayTimeUnit = TimeUnit.MILLISECONDS,
    terminationDelay = 10,
    terminationDelayTimeUnit = TimeUnit.SECONDS
  )
  val push: Event[CCC] => Future[Unit] = { a =>
    asyncQueue.push(a).fold(Future.failed, Future.successful)
  }

  val idx = new AtomicInteger(0)
  val xs: Vector[Event[CCC]] =
    Iterator.from(1).map(x => Event("example", "ccc", ccc.copy(i = idx.getAndIncrement()))).take(args(3).toInt).toVector

  val wokers = new AtomicInteger(1)

  val scheduler: ScheduledExecutorService = Executors.newScheduledThreadPool(1)
  val worker = new Runnable {
    override def run(): Unit = {
      println(("worker", "queue has remaining", asyncQueue.size))
      val start = System.nanoTime()
      val r = Await.result(xs traverse push attempt, Inf)
      val elapsed = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - start)
      println(("worker", wokers.getAndIncrement(), r.isRight, elapsed))
    }
  }

  val pool: ExecutorService = Executors.newWorkStealingPool()
  scheduler.scheduleWithFixedDelay(new Runnable {
    override def run(): Unit = pool.execute(worker)
  }, 0, 1, TimeUnit.SECONDS)

  scheduler.awaitTermination(args(2).toLong, TimeUnit.SECONDS)
  scheduler.shutdownNow()
  pool.shutdown()
  asyncQueue.close()
}
