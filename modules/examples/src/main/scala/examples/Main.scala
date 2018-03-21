package examples

import java.net.InetSocketAddress
import java.time.{Clock, Duration}
import java.util.concurrent._
import java.util.concurrent.atomic.AtomicLong

import com.typesafe.scalalogging.LazyLogging
import fluflu._
import fluflu.queue.Client
import fluflu.msgpack.circe._
import io.circe.generic.auto._
import _root_.monix.execution.Scheduler
import _root_.monix.eval.Task
import _root_.monix.reactive.{Consumer, Observable}

import scala.concurrent.Await
import scala.concurrent.duration._
import scala.util.Random

/**
  * sbt "examples/runMain examples.Main 10 100"
  */
object Main extends Base {
  import TimeUnit._

  def main(args: Array[String]): Unit = {
    val counter = new AtomicLong(0)
    val start   = System.nanoTime()

    val seconds = args(0).toLong
    val count   = args(1).toLong

    val observable = Observable
      .repeatEval(counter.getAndIncrement())
      .take(count)

    val consumer = Consumer.foreachParallelTask(10) { x: Long =>
      Task(client.emit(s"docker.main.${x % 3}", Num(x)))
    }

    implicit val s: Scheduler = Scheduler.computation(10)

    val f = consumer.apply(observable).runAsync

    TimeUnit.SECONDS.sleep(seconds)

    try Await.result(f, 1.milli)
    catch {
      case _: TimeoutException => ()
    }

    client.close()

    logger.info(s"Elapsed: ${NANOSECONDS.toMillis(System.nanoTime() - start)} ms.")
  }
}

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

  val host = sys.props.getOrElse("fluentd.host", "localhost")
  val port = sys.props.getOrElse("fluentd.port", "24224").toInt
  implicit val connection: Connection = Connection(
    remote = new InetSocketAddress(host, port),
    timeout = Duration.ofSeconds(10),
    Backoff.exponential(Duration.ofNanos(500), Duration.ofSeconds(10), rnd)
  )

  val client: Client = {
    import fluflu.time.eventTime._

    implicit val consumeScheduler: Scheduler =
      Scheduler.singleThread(name = "fluflu-example")

    implicit val messenger: Messenger = new monix.MessengerTask(
      timeout = Duration.ofSeconds(10),
      backoff = Backoff.exponential(Duration.ofNanos(500), Duration.ofSeconds(5), rnd)
    )

    Client.forwardable(
      delay = Duration.ofNanos(50),
      terminationDelay = Duration.ofSeconds(10),
      maximumPulls = 5000
    )
  }
}

final case class Num(n: Long)

final case class CCC(
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
