package fluflu

import java.nio.ByteBuffer

import cats.data.Xor
import data.Event
import io.circe.Encoder

import scala.concurrent.{ ExecutionContext, Future }

final case class Writer(
    host: String,
    port: Int,
    clientPoolSize: Int = 1,
    messengerPoolSize: Int = 1,
    maxConnectionRetries: Int = 10,
    maxWriteRetries: Int = 10,
    reconnectionBackoff: Backoff,
    rewriteBackoff: Backoff
) {

  private[this] val client = Client(
    host,
    port,
    clientPoolSize,
    messengerPoolSize,
    maxConnectionRetries,
    maxWriteRetries,
    reconnectionBackoff,
    rewriteBackoff
  )

  def die: Boolean = client.die

  def write[A](e: Event[A])(implicit A: Encoder[A]): Throwable Xor Unit =
    Message.pack(e).map(msg => client enqueue Letter(msg, 0))

  def writeFuture[A](e: Event[A])(implicit A: Encoder[A], E: ExecutionContext): Future[Throwable Xor Unit] =
    Future(Message pack e) map (packed => packed map (msg => client enqueue Letter(msg, 0)))

  def close(): Unit = client.close()
}

final case class Letter(message: ByteBuffer, retries: Int)
