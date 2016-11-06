package fluflu

import java.time.{ Clock, Duration, Instant }

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
  def write[A: Encoder](e: Event[A])(implicit ec: ExecutionContext): Future[Unit] =
    Future(Message pack e) map (packed => packed map (msg => messenger.write(Letter(msg), 0, Instant.now(clock))))

  def close(): Unit = {
    messenger.close()
  }
}
