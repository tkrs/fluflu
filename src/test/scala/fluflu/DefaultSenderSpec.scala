package fluflu

import java.net.InetSocketAddress
import java.nio._
import java.nio.channels._
import java.util.concurrent.ForkJoinPool
import java.util.concurrent.atomic.AtomicInteger

import org.scalatest._

import scala.annotation.tailrec

class DefaultSenderSpec extends FlatSpec with Matchers with BeforeAndAfter {

  var port: AtomicInteger = new AtomicInteger()

  def runServer(p: Option[Int] = None) = new Runnable {
    @tailrec
    def handler(ch: ServerSocketChannel): Unit = {
      val buffer = ByteBuffer.allocate(1024)
      val client = ch.accept()
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
    pool.execute(runServer())

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

    pool.execute(runServer())
    Thread.sleep(500)

    val sender = DefaultSender("localhost", port.get)

    pool.shutdownNow()

    {
      val pool = new ForkJoinPool()
      pool.execute(runServer(Some(port.get)))

      @tailrec
      def run(buf: ByteBuffer, retries: Seq[Long]): Int = retries match {
        case Nil => fail()
        case h :: t => try {
          sender.write(buf)
        } catch {
          case e: Throwable =>
            println(s"${e}")
            Thread.sleep(h)
            run(buf, t)
        }
      }

      val retries = Seq(100L, 500L, 1000L)
      val bytes = "TEST2".getBytes("UTF8")
      val buffer = ByteBuffer.wrap(bytes)
      val ret = run(buffer, retries)

      ret shouldEqual bytes.length

      pool.shutdownNow()
    }
  }
}
