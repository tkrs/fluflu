package examples

import java.net.InetSocketAddress
import java.time.Clock
import java.util.concurrent._
import java.util.concurrent.atomic.AtomicLong

import com.typesafe.scalalogging.LazyLogging
import fluflu._
import fluflu.msgpack.mess._
import mess.Encoder
import mess.codec.generic._
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

  def main(args: Array[String]): Unit = {
    val counter = new AtomicLong(0)
    val start   = System.nanoTime()

    val seconds = args(0).toLong
    val count   = args(1).toLong

    val observable = Observable
      .repeatEval(counter.getAndIncrement())
      .take(count)

    val consumer = Consumer.foreachParallelTask(16) { x: Long =>
      val xx = x % 10
      Task[Unit](client.emit(s"docker.main.$xx", Num(x))).delayExecution((xx * 20).micros)
    // Task(client.emit(s"docker.main.${x % 10}", genFoo(x)))
    }

    implicit val s: Scheduler = Scheduler.computation(4)

    val f = consumer.apply(observable).runAsync

    val monitor = new Thread(new Runnable {
      def run(): Unit =
        do {
          Thread.sleep(1000)
          logger.info(s"Remaining: ${client.size}")
        } while (!client.isClosed)
    })
    monitor.start()

    SECONDS.sleep(seconds)

    try Await.result(f, 1.milli)
    catch {
      case _: TimeoutException => ()
    }

    logger.info("Time is up.")

    client.close()

    logger.info("client closed.")

    logger.info(s"Remaining: ${client.size}")

    logger.info(s"Elapsed: ${NANOSECONDS.toMillis(System.nanoTime() - start)} ms, emit count: ${counter.get}")
  }
}

abstract class Base extends LazyLogging {

  val rnd: Random = new Random(System.nanoTime())

  def genFoo(index: Long): Foo = Foo(
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

  val addr = new InetSocketAddress(host, port)

  val connSettings = Connection.Settings(
    60.seconds,
    Backoff.exponential(500.nanos, 10.seconds, rnd),
    10.seconds,
    Backoff.exponential(500.nanos, 10.seconds, rnd),
    10.seconds,
    Backoff.exponential(500.nanos, 10.seconds, rnd)
  )

  implicit val connection: Connection = Connection(addr, connSettings)

  val client: Client = {
    import msgpack.time.eventTime._

    Client(
      terminationTimeout = 10.seconds,
      maximumPulls = 5000
    )
  }
}

final case class Num(n: Long)
object Num {
  implicit val encodeNum: Encoder[Num] = derivedEncoder[Num]
}

final case class Foo(
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
object Foo {
  implicit val encodeFoo: Encoder[Foo] = derivedEncoder[Foo]
}
