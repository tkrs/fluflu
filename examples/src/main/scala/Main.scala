import java.net.InetSocketAddress
import java.time.{ Clock, Duration }
import java.util.concurrent._
import java.util.concurrent.atomic.AtomicLong

import com.typesafe.scalalogging.LazyLogging
import fluflu._
import fluflu.queue.Client
import io.circe.generic.auto._
import monix.execution.Scheduler

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

/**
 * sbt "examples/runMain Main 127.0.0.1 24224 10 100"
 */
object Main extends LazyLogging {
  import TimeUnit._

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

  def main(args: Array[String]): Unit = {
    implicit val connection = Connection(
      remote = new InetSocketAddress(args(0), args(1).toInt),
      timeout = Duration.ofSeconds(10),
      Backoff.exponential(Duration.ofNanos(500), Duration.ofSeconds(10), rnd)
    )

    implicit val messenger: Messenger = Messenger(
      timeout = Duration.ofSeconds(10),
      backoff = Backoff.exponential(Duration.ofNanos(500), Duration.ofSeconds(5), rnd)
    )

    implicit val consumeScheduler: Scheduler =
      Scheduler.computation(parallelism = 1, name = "fluflu-example")

    val client: Client = Client(
      delay = Duration.ofMillis(50),
      terminationDelay = Duration.ofSeconds(10)
    )

    object R extends Runnable {
      val sec = args(2).toLong
      val len = args(3).toLong
      val delay = SECONDS.toNanos(sec) / len

      val pool = Executors.newSingleThreadScheduledExecutor()
      val executor = Executors.newFixedThreadPool(4)

      def init(): Unit = ()

      def start: ScheduledFuture[_] = {
        logger.info("Start consuming thread.")
        _start(0)
      }

      private def _start(delay: Long): ScheduledFuture[_] =
        pool.schedule(this, delay, NANOSECONDS)

      val counter = new AtomicLong(0)

      def run(): Unit = {
        if (counter.get < len.toLong) {
          val e = Event("example", "ccc", ccc(counter.getAndIncrement()))
          client.emit(e)
          _start(delay)
        }
      }

      def await(): Unit = {
        if (!pool.awaitTermination(SECONDS.toNanos(sec), NANOSECONDS))
          pool.shutdownNow()

        executor.shutdown()
        executor.shutdownNow()

        logger.info(s"Emitted count: ${counter.get}")
        // if (!q.isEmpty) logger.info(s"This example app has remaining data: ${q.size}.")
      }
    }

    logger.info("Start initialize test date.")

    R.init()

    logger.info("Finish initialize test date.")

    val start = System.nanoTime()

    R.start
    R.start

    logger.info("...")

    R.await()

    client.close()

    val remaining = client.remaining
    if (remaining > 0) logger.info(s"Client has remaining data: ${client.remaining}.")

    logger.info(s"Elapsed: ${NANOSECONDS.toMillis(System.nanoTime() - start)} ms.")
  }
}
