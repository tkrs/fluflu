package fluflu

import java.io.IOException
import java.net.InetSocketAddress
import java.nio._
import java.nio.channels._
import java.util.concurrent.{ TimeUnit, ForkJoinPool }
import java.util.concurrent.atomic.AtomicInteger

import org.scalatest._

import scala.annotation.tailrec

class DefaultSenderSpec extends FlatSpec with Matchers with BeforeAndAfter {

  var port: AtomicInteger = new AtomicInteger()

  def runServer(p: Option[Int]) = new Runnable {
    @tailrec
    def handler(ch: ServerSocketChannel): Unit = {
      val client = ch.accept()
      val buffer = ByteBuffer.allocate(1024)
      client.read(buffer)
      val msg = new String(buffer.array(), "UTF8")

      println(s"recieve => ${msg}")

      handler(ch)

    }
    override def run(): Unit = {
      val ch = ServerSocketChannel.open()
      val sock = ch.socket()
      p match {
        case Some(v) => sock.bind(new InetSocketAddress("localhost", v))
        case _ => sock.bind(null)
      }
      port.set(sock.getLocalPort)

      try { handler(ch) } catch { case e: Throwable => () }
    }
  }

  it should "return buffer length when successful socket write" in {

    val pool = new ForkJoinPool()
    pool.execute(runServer(None))

    Thread.sleep(500)

    val sender = DefaultSender("localhost", port.get)

    {
      val bytes = "TEST1".getBytes("UTF8")
      val buffer = ByteBuffer.wrap(bytes)

      val ret = sender.write(buffer)

      ret shouldEqual bytes.length
    }

    sender.close()

    pool.shutdownNow()

  }

  it should "throw exception when shutdowned the connected server" in {

    val pool = new ForkJoinPool()

    pool.execute(runServer(None))
    Thread.sleep(500)

    val sender = DefaultSender("localhost", port.get)

    pool.shutdownNow()

    {
      val pool = new ForkJoinPool()
      pool.execute(runServer(Some(port.get)))

      {
        val bytes = "TEST2".getBytes("UTF8")

        val ret = Seq(100L, 500L, 1000L, 1500L).map { t =>
          try {
            val buffer = ByteBuffer.wrap(bytes)
            sender.write(buffer)
          } catch {
            case e: Throwable =>
              println(s"${t} => ${e}")
              Thread.sleep(t)
              0
          }
        }

        ret.last shouldEqual bytes.length

      }
      pool.shutdownNow()
    }
  }
}
