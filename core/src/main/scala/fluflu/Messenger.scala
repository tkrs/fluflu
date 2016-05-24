package fluflu

import java.util.concurrent._

import scala.util.Try

final case class Messenger(
    conn: Connection,
    letterQueue: ConcurrentLinkedDeque[Letter],
    timeout: Long,
    maximumPoolSize: Int,
    retryHandler: (Letter) => PartialFunction[Throwable, Unit]
) extends Callable[Unit] {

  private[this] val blockingDuration: Long = 50
  private[this] val pool = Executors.newWorkStealingPool(maximumPoolSize)

  def enqueue(letter: Letter): Unit = letterQueue.addLast(letter)
  def enqueueFront(letter: Letter): Unit = letterQueue.addFirst(letter)

  override def call(): Unit =
    try {
      while (true) {
        if (!conn.isClosed)
          Option(letterQueue.poll()) match {
            case None =>
              blocker(blockingDuration)
            case Some(letter) =>
              Try(pool.submit(new Callable[Unit] {
                override def call(): Unit = conn.write(letter.message)
              }).get(timeout, TimeUnit.MILLISECONDS)).recover(retryHandler(letter))
          }
        else
          conn.connect()
      }
    } catch {
      case e: InterruptedException => // nop
    }

  def close(): Unit = {
    pool.shutdown()
  }
}
