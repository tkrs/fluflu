package fluflu

import java.io.IOException
import java.net.InetSocketAddress
import java.nio.channels._
import java.util.concurrent._

final case class Client(
    host: String,
    port: Int,
    timeout: Long,
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
  private[this] val pool = Executors.newWorkStealingPool(clientPoolSize)
  private[this] val messenger = Messenger(connection, letterQueue, timeout, consumerPoolSize, handleRetry)
  pool.submit(messenger)

  def die = connection.noLongerRetriable

  def enqueue(letter: Letter): Unit = messenger.enqueue(letter)

  private[this] def retry(letter: Letter): Unit =
    if (letter.retries < maxWriteRetries) {
      blocker(rewriteBackoff.nextDelay(letter.retries))
      letter.message.flip()
      messenger.enqueueFront(letter.copy(retries = letter.retries + 1))
    }

  private[this] def handleRetry(letter: Letter): PartialFunction[Throwable, Unit] = {
    case ee: ExecutionException => ee.getCause match {
      case e: NotYetConnectedException => retry(letter)
      case e: IOException => retry(letter)
    }
    case e: TimeoutException => retry(letter)
  }

  def close(): Unit = {
    pool.shutdown()
    messenger.close()
    connection.close()
  }
}
