package fluflu

import java.io.IOException
import java.net.InetSocketAddress
import java.nio.channels._
import java.util.concurrent._

import scala.concurrent.blocking

final case class Client(
    host: String,
    port: Int,
    clientPoolSize: Int,
    consumerPoolSize: Int,
    maxConnectRetries: Int,
    maxWriteRetries: Int,
    reconnectionBackoff: Backoff,
    rewriteBackoff: Backoff
) {

  private[this] val dest = new InetSocketAddress(host, port)
  private[this] val letterQueue: ConcurrentLinkedDeque[Letter] = new ConcurrentLinkedDeque
  private[this] val connection = Connection(dest, maxConnectRetries, reconnectionBackoff)
  private[this] val pool = Executors.newCachedThreadPool
  private[this] val messenger = Messenger(connection, letterQueue, consumerPoolSize, handleRetry)
  pool.submit(messenger)

  def die: Boolean = connection.noLongerRetriable

  def enqueue(letter: Letter): Unit = messenger.enqueue(letter)

  private[this] def retry(letter: Letter): Unit =
    if (letter.retries < maxWriteRetries) {
      blocking {
        TimeUnit.NANOSECONDS.sleep(rewriteBackoff.nextDelay(letter.retries).toNanos)
      }
      letter.message.flip()
      messenger.enqueueFront(letter.copy(retries = letter.retries + 1))
    }

  private[this] def handleRetry(letter: Letter): PartialFunction[Throwable, Unit] = {
    case e: NotYetConnectedException => retry(letter)
    case e: IOException => retry(letter)
  }

  def close(): Unit = {
    pool.awaitTermination(10, TimeUnit.SECONDS)
    pool.shutdownNow()
    connection.close()
  }
}
