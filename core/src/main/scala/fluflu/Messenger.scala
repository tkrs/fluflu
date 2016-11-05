package fluflu

import java.net.InetSocketAddress
import java.time.{ Clock, Duration, Instant }
import java.util.concurrent._

import scala.annotation.tailrec
import scala.concurrent.blocking
import scala.util.{ Failure, Success }

final case class Messenger(
    host: String,
    port: Int,
    reconnectionTimeout: Duration,
    rewriteTimeout: Duration,
    reconnectionBackoff: Backoff,
    rewriteBackoff: Backoff
)(implicit clock: Clock) {

  private[this] val blockingDuration: Long = 50
  private[this] val dest = new InetSocketAddress(host, port)
  private[this] val letterQueue: BlockingDeque[Letter] = new LinkedBlockingDeque[Letter]()
  private[this] val connection = Connection(dest, reconnectionTimeout, reconnectionBackoff)
  private[this] val executor = Executors.newSingleThreadExecutor()

  executor.execute(new Runnable {
    @tailrec def doWrite(letter: Letter, retries: Int, start: Instant): Unit =
      connection.write(letter.message) match {
        case Failure(e) =>
          if (Instant.now(clock).minusNanos(rewriteTimeout.toNanos).compareTo(start) <= 0) {
            blocking {
              TimeUnit.NANOSECONDS.sleep(rewriteBackoff.nextDelay(letter.retries).toNanos)
            }
            letter.message.flip()
            doWrite(letter, retries + 1, start)
          }
        case Success(_) => ()
      }

    @tailrec def write(): Unit =
      if (executor.isShutdown) () else {
        if (!connection.isClosed)
          Option(letterQueue.poll()) match {
            case None =>
              blocking(TimeUnit.NANOSECONDS.sleep(blockingDuration))
            case Some(letter) =>
              doWrite(letter, 0, Instant.now(clock))
          }
        else
          connection.connect()
        write()
      }

    override def run(): Unit = write()
  })

  def die: Boolean = connection.noLongerRetriable

  def enqueue(letter: Letter): Unit = letterQueue.addLast(letter)

  def close(): Unit = {
    executor.shutdown()
    executor.awaitTermination(10, TimeUnit.SECONDS)
    if (!executor.isTerminated) executor.shutdownNow()
    connection.close()
  }
}
