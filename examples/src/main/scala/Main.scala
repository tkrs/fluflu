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

  val ccc: CCC = CCC(0, "foo", "", Int.MaxValue, Map("name" -> "fluflu"), Seq(1.2, Double.MaxValue, Double.MinValue))

  val messenger = fluflu.DefaultMessenger(
    host = args(0),
    port = args(1).toInt,
    reconnectionTimeout = Duration.ofSeconds(10),
    rewriteTimeout = Duration.ofSeconds(10),
    reconnectionBackoff = reconnectionBackoff,
    rewriteBackoff = rewriteBackoff
  )
  val writer: Async = Async(
    messenger = messenger,
    initialBufferSize = 1024,
    initialDelay = 500,
    delay = 500,
    delayTimeUnit = TimeUnit.MILLISECONDS,
    terminationDelay = 10,
    terminationDelayTimeUnit = TimeUnit.SECONDS
  )
  val write: Event[CCC] => Future[Unit] = { a =>
    writer.push(a).fold(Future.failed, Future.successful)
  }
  val xs: Vector[Event[CCC]] =
    Iterator.from(1).map(x => Event("example", "ccc", ccc.copy(i = x))).take(5000).toVector

  val i = new AtomicInteger(1)

  val scheduler: ScheduledExecutorService = Executors.newScheduledThreadPool(1)
  val worker = new Runnable {
    override def run(): Unit = {
      val start = System.nanoTime()
      val r = Await.result(xs traverse write attempt, Inf)
      val elapsed = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - start)
      println(("worker", i.getAndIncrement(), r.isRight, elapsed))
    }
  }

  val pool: ExecutorService = Executors.newWorkStealingPool()
  scheduler.scheduleWithFixedDelay(new Runnable {
    override def run(): Unit = pool.execute(worker)
  }, 0, 1, TimeUnit.SECONDS)

  scheduler.awaitTermination(args(2).toLong, TimeUnit.SECONDS)
  scheduler.shutdownNow()
  pool.shutdown()
  writer.close()
}
