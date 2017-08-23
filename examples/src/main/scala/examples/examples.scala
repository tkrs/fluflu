package examples

import java.net.InetSocketAddress
import java.time.{Clock, Duration}
import java.util.concurrent._
import java.util.concurrent.atomic.AtomicLong

import com.typesafe.scalalogging.LazyLogging
import fluflu._
import fluflu.queue.Client
import io.circe.generic.auto._
import monix.eval.Task
import monix.execution.Scheduler
import monix.reactive.{Consumer, Observable}

import scala.util.Random

case class CCC(
    i: Long,
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

abstract class Base extends LazyLogging {

  val rnd: Random = new Random(System.nanoTime())

  def ccc(index: Long): CCC = CCC(
    index,
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

  implicit val clock: Clock = Clock.systemUTC()

  val host = sys.env.getOrElse("FLUENTD_HOST", "localhost")
  val port = sys.env.getOrElse("FLUENTD_PORT", "24224").toInt
  implicit val connection: Connection = Connection(
    remote = new InetSocketAddress(host, port),
    timeout = Duration.ofSeconds(10),
    Backoff.exponential(Duration.ofNanos(500), Duration.ofSeconds(10), rnd)
  )

  val client: Client = {

    implicit val messenger: Messenger = Messenger(
      timeout = Duration.ofSeconds(10),
      backoff = Backoff.exponential(Duration.ofNanos(500), Duration.ofSeconds(5), rnd)
    )

    implicit val consumeScheduler: Scheduler =
      Scheduler.singleThread(name = "fluflu-example")

    Client(
      delay = Duration.ofNanos(500),
      terminationDelay = Duration.ofSeconds(10)
    )
  }
}

/**
  * sbt "examples/runMain examples.Scheduling 10 100"
  */
object Scheduling extends Base {
  import TimeUnit._

  def main(args: Array[String]): Unit = {

    object R extends Runnable {
      private[this] val sec = args(0).toLong
      private[this] val len = args(1).toLong
      private[this] val delay = SECONDS.toNanos(sec) / len
      private[this] val counter = new AtomicLong(0)
      private[this] val emitter = Executors.newSingleThreadScheduledExecutor()

      override def run(): Unit = {
        if (counter.get < len.toLong) {
          // val e = Event("example", "schedule", ccc(counter.getAndIncrement()))
          val e = Event("example", "schedule", counter.getAndIncrement())
          client.emit(e)
          _start(delay)
        }
      }

      def start: ScheduledFuture[_] = {
        logger.info("Start consuming thread.")
        _start(0)
      }

      def await(): Unit = {
        if (!emitter.awaitTermination(SECONDS.toNanos(sec), NANOSECONDS))
          emitter.shutdownNow()

        logger.info(s"Emitted count: ${counter.get}")
      }

      private def _start(delay: Long): ScheduledFuture[_] =
        emitter.schedule(this, delay, NANOSECONDS)
    }

    val start = System.nanoTime()

    R.start
    R.await()

    client.close()

    val remaining = client.remaining
    if (remaining > 0) logger.info(s"Client has remaining data: ${client.remaining}.")

    logger.info(s"Elapsed: ${NANOSECONDS.toMillis(System.nanoTime() - start)} ms.")
  }
}

/**
  * sbt "examples/runMain examples.Counter 5000"
  */
object Counter extends Base {
  import scala.concurrent.Await
  import scala.concurrent.duration._

  def main(args: Array[String]): Unit = {

    implicit val scheduler = Scheduler.computation(daemonic = false)

    val counter = new AtomicLong(0)

    val observable = Observable.repeatEval(Event("example", "counter", counter.getAndIncrement()))

    val consumer = Consumer.foreachParallelAsync[Event[Long]](4)(event =>
      Task(client.emit(event) match {
        case Left(e) => logger.error(s"Exception occurred: ${e.getMessage}")
        case _       => ()
      }))

    val task = observable.consumeWith(consumer)

    logger.info("Start emitting.")
    try Await.result(task.runAsync, args(0).toLong.millis)
    catch {
      case e: TimeoutException => ()
      case e: Throwable        => e.printStackTrace()
    } finally {
      client.close()
    }
  }
}
