package examples

import java.net.InetSocketAddress
import java.time._

import com.typesafe.scalalogging.LazyLogging
import fluflu._
import fluflu.msgpack.mess._
import mess.Encoder
import mess.ast.MsgPack

import scala.concurrent.duration._
import scala.util.Random

/**
  * sbt "examples/runMain examples.Main"
  */
object Main extends LazyLogging {

  implicit val encodeLocalDate: Encoder[LocalDate] =
    a => MsgPack.fromLong(a.toEpochDay)

  def main(args: Array[String]): Unit = {

    implicit val clock: Clock = Clock.systemUTC()

    val host = sys.props.getOrElse("fluentd.host", "localhost")
    val port = sys.props.getOrElse("fluentd.port", "24224").toInt
    val addr = new InetSocketAddress(host, port)

    val rnd = new Random()
    val connSettings = Connection.Settings(
      60.seconds,
      Backoff.exponential(500.nanos, 10.seconds, rnd),
      10.seconds,
      Backoff.exponential(500.nanos, 10.seconds, rnd),
      10.seconds,
      Backoff.exponential(500.nanos, 10.seconds, rnd)
    )
    implicit val connection: Connection = Connection(addr, connSettings)

    import msgpack.time.eventTime._

    val client: Client = Client(
      terminationTimeout = 10.seconds,
      maximumPulls = 5000
    )

    client.emit("human.japanese", Human(LocalDate.of(1984, 6, 30), "Takeru Sato")) match {
      case Right(_) => ()
      case Left(e)  => e.printStackTrace()
    }

    client.close()
  }
}

final case class Human(dayOfbirth: LocalDate, name: String)
