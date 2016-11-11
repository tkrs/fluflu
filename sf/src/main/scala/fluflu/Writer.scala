package fluflu

import java.nio.ByteBuffer
import java.time.{ Clock, Instant }

import io.circe.Encoder

import scala.concurrent.{ ExecutionContext, Future }

final case class Writer(messenger: Messenger)(implicit clock: Clock) {

  def write[A: Encoder](e: Event[A])(implicit ec: ExecutionContext): Future[Unit] =
    Future(Message pack e) map (packed => packed map (msg => messenger.write(ByteBuffer.wrap(msg), 0, Instant.now(clock))))

  def close(): Unit = {
    messenger.close()
  }
}
