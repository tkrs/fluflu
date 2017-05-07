import java.net.InetSocketAddress
import java.time.{ Clock, Duration }
import java.util.concurrent._

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
object Main extends LazyLogging {

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

  // println(ccc)

  implicit val clock: Clock = Clock.systemUTC()

  val pool = Executors.newScheduledThreadPool(2)
  val executor = Executors.newFixedThreadPool(4)

  def main(args: Array[String]): Unit = {
    implicit val connection = Connection(
      remote = new InetSocketAddress(args(0), args(1).toInt),
      timeout = Duration.ofSeconds(10),
      Backoff.exponential(Duration.ofNanos(500), Duration.ofSeconds(10), rnd)
    )

    implicit val messenger: Messenger = Messenger(
      parallelism = 10,
      timeout = Duration.ofSeconds(10),
      backoff = Backoff.exponential(Duration.ofNanos(500), Duration.ofSeconds(5), rnd),
      terminationDelay = Duration.ofSeconds(10)
    )

    val asyncQueue: Async = Async(
      delay = Duration.ofMillis(50),
      terminationDelay = Duration.ofSeconds(10)
    )

    val sec = args(2).toLong
    val len = args(3).toInt

    val q: BlockingQueue[Event[CCC]] = new ArrayBlockingQueue(len)

    Iterator.from(1)
      .map(i => Event("example", "ccc", ccc.copy(i = i)))
      .take(len)
      .foreach(x => q.offer(x))

    val delay = TimeUnit.SECONDS.toNanos(sec) / len

    object R extends Runnable {
      def start: ScheduledFuture[_] =
        pool.schedule(this, delay, TimeUnit.NANOSECONDS)

      def run(): Unit = executor.execute(() => {
        val e = q.poll()
        if (e != null) {
          asyncQueue.emit(e)
          if (!q.isEmpty()) start
        }
      })
    }

    logger.info("Start")
    R.start
    R.start
    R.start

    val start = System.nanoTime()
    if (!pool.awaitTermination(TimeUnit.SECONDS.toNanos(sec) + delay, TimeUnit.NANOSECONDS))
      pool.shutdownNow()

    executor.shutdown()
    executor.shutdownNow()

    logger.info(s"Queue remaining: ${asyncQueue.remaining}")
    logger.info(s"Test data remaining: ${q.size}")

    logger.info(s"Elapsed: ${TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - start)} ms")

    asyncQueue.close()
  }
}
