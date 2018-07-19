package fluflu.connection

import java.io.IOException
import java.time.Duration
import java.net.InetSocketAddress
import java.nio.ByteBuffer

import fluflu.msgpack.{Ack, Unpacker}
import fluflu.{Backoff, Connection}
import fluflu.msgpack.circe._
import org.scalatest.FlatSpec

class ConnectionSpec extends FlatSpec {
  import fluflu.msgpack.MsgpackHelper._

  val connSettings = Connection.Settings(
    Duration.ofSeconds(1),
    Backoff.fix(Duration.ofMillis(1)),
    Duration.ofSeconds(1),
    Backoff.fix(Duration.ofMillis(1)),
    Duration.ofSeconds(1),
    Backoff.fix(Duration.ofMillis(1))
  )

  val message =
    x"93 A8 74 61 67 2E 6E 61 6D 65 93 92 CE 55 EC E6 F8 81 A7 6D 65 73 73 61 67 65 A3 66 6F 6F 92 CE 55 EC E6 F9 81 A7 6D 65 73 73 61 67 65 A3 62 61 72 92 CE 55 EC E6 FA 81 A7 6D 65 73 73 61 67 65 A3 62 61 7A 81 A5 63 68 75 6E 6B D9 34 59 57 4A 6C 5A 6A 41 30 59 32 55 74 4E 57 51 77 4F 53 30 30 4E 47 4E 6A 4C 54 67 33 4F 54 59 74 4D 47 59 35 5A 54 46 6C 4E 44 56 68 4F 47 59 34 43 67 3D 3D"

  it should "write successfully" in {
    val inet = new InetSocketAddress(24224)
    val conn = Connection(inet, connSettings)
    assert(!conn.isClosed)
    val ackR = conn.writeAndRead(ByteBuffer.wrap(message)).get
    val ack  = implicitly[Unpacker[Option[Ack]]].apply(ackR).right.get
    assert(ack.get.ack === "YWJlZjA0Y2UtNWQwOS00NGNjLTg3OTYtMGY5ZTFlNDVhOGY4Cg==")
  }

  it should "written to be failed when its connection was closed" in {
    val inet = new InetSocketAddress(24224)
    val conn = Connection(inet, connSettings)
    conn.close().get
    assert(conn.isClosed)
    assert(conn.writeAndRead(ByteBuffer.wrap(message)).isFailure)
  }

  "Constructor" should "throw IOException when its connecting to a server was failed" in {
    val inet = new InetSocketAddress(25)
    assertThrows[IOException](Connection(inet, connSettings))
  }
}
