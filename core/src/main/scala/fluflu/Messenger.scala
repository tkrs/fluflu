package fluflu

import java.util.concurrent._

import scala.concurrent.blocking
import scala.util.Try

final case class Messenger(
    conn: Connection,
    letterQueue: ConcurrentLinkedDeque[Letter],
    maximumPoolSize: Int,
    retryHandler: Letter => PartialFunction[Throwable, Unit]
) extends Callable[Unit] {

  private[this] val blockingDuration: Long = 50

  def enqueue(letter: Letter): Unit = letterQueue.addLast(letter)
  def enqueueFront(letter: Letter): Unit = letterQueue.addFirst(letter)

  override def call(): Unit =
    while (true) {
      if (!conn.isClosed)
        Option(letterQueue.poll()) match {
          case None =>
            blocking(TimeUnit.NANOSECONDS.sleep(blockingDuration))
          case Some(letter) =>
            Try(conn.write(letter.message)).recover(retryHandler(letter))
        }
      else
        conn.connect()
    }
}
