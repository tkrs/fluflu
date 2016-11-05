package fluflu

import java.time.{ Clock, Duration }

import cats.data.Xor
import data.Event
import io.circe.Encoder

import scala.concurrent.{ ExecutionContext, Future }

final case class Writer(
    host: String = "127.0.0.1",
    port: Int = 24224,
    reconnectionTimeout: Duration,
    rewriteTimeout: Duration,
    reconnectionBackoff: Backoff,
    rewriteBackoff: Backoff
)(implicit clock: Clock) {

  private[this] val messenger = Messenger(
    host,
    port,
    reconnectionTimeout,
    rewriteTimeout,
    reconnectionBackoff,
    rewriteBackoff
  )

  def die: Boolean = messenger.die

  def write[A: Encoder](e: Event[A]): Throwable Xor Unit =
    Message.pack(e).map(msg => messenger enqueue Letter(msg, 0))

  def writeFuture[A: Encoder](e: Event[A])(implicit ec: ExecutionContext): Future[Throwable Xor Unit] =
    Future(Message pack e) map (packed => packed map (msg => messenger enqueue Letter(msg, 0)))

  def close(): Unit = messenger.close()
}
