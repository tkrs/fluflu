package fluflu

import java.time.{ Clock, Instant }

import cats.syntax.either._
import io.circe.Encoder

import scala.concurrent.{ ExecutionContext, Future }
import scala.util.{ Either => \/ }

final case class Writer[P](connection: Connection[P])(implicit clock: Clock, P: Message[P]) {

  def write[A: Encoder](e: Event[A])(implicit ec: ExecutionContext): Future[Throwable \/ Unit] =
    Future(P(e).map(p => connection.write(p, 0, Instant.now(clock))))

  def close(): Unit = {
    connection.close()
  }
}
