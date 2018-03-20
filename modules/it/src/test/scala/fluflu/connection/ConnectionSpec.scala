package fluflu.connection

import java.io.IOException
import java.time.Duration
import java.net.InetSocketAddress
import java.nio.ByteBuffer

import fluflu.{Backoff, Connection}
import org.scalatest.FlatSpec

class ConnectionSpec extends FlatSpec {
  import fluflu.msgpack.MsgpackHelper._

  val message =
    x"92 a8 74 61 67 2e 6e 61 6d 65 93 92 ce 55 ec e6 f8 81 a7 6d 65 73 73 61 67 65 a3 66 6f 6f 92 ce 55 ec e6 f9 81 a7 6d 65 73 73 61 67 65 a3 62 61 72 92 ce 55 ec e6 fa 81 a7 6d 65 73 73 61 67 65 a3 62 61 7a"

  it should "write successfully" in {
    val inet = new InetSocketAddress(24224)
    val conn = Connection(inet, Duration.ofSeconds(1), Backoff.fix(Duration.ofSeconds(1)))
    assert(!conn.isClosed)
    assert(conn.write(ByteBuffer.wrap(message)).isSuccess)
  }

  it should "written to be failed when its connection was closed" in {
    val inet = new InetSocketAddress(24224)
    val conn = Connection(inet, Duration.ofSeconds(1), Backoff.fix(Duration.ofSeconds(1)))
    conn.close().get
    assert(conn.isClosed)
    assert(conn.write(ByteBuffer.wrap(message)).isFailure)
  }

  "Constructor" should "throw IOException when its connecting to a server was failed" in {
    val inet = new InetSocketAddress(25)
    assertThrows[IOException](
      Connection(inet, Duration.ofSeconds(1), Backoff.fix(Duration.ofMillis(1))))
  }
}
