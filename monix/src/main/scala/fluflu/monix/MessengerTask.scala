package fluflu.monix

import java.io.IOException
import java.nio.ByteBuffer
import java.time.{Clock, Duration, Instant}

import cats.syntax.either._
import com.typesafe.scalalogging.LazyLogging
import fluflu.{Backoff, Connection, Messenger}
import _root_.monix.eval.{Callback, Task}
import _root_.monix.execution.Scheduler

import scala.concurrent.duration._

final class MessengerTask(timeout: Duration, backoff: Backoff)(implicit connection: Connection,
                                                               taskScheduler: Scheduler,
                                                               clock: Clock)
    extends Messenger
    with LazyLogging {

  def emit(elms: Iterator[Elm]): Unit = {
    def go(buffer: ByteBuffer, retries: Int, start: Instant): Task[Unit] =
      Task
        .fromTry(connection.write(buffer))
        .flatMap { _ =>
          if (!buffer.hasRemaining) Task.unit
          else go(buffer, retries, start)
        }
        .onErrorRecoverWith {
          case e: IOException =>
            buffer.flip()
            if (giveup(start))
              Task.raiseError(e)
            else
              go(buffer, retries + 1, start)
                .delayExecution(backoff.nextDelay(retries).toNanos.nanos)
        }

    val tasks = elms.map { elm =>
      elm().map(l => ByteBuffer.wrap(l.message)) match {
        case Left(e) =>
          logger.warn(e.getMessage)
          Task.unit
        case Right(b) =>
          go(b, 0, Instant.now(clock))
      }
    }

    Task
      .gatherUnordered(tasks)
      .runAsync(new Callback[List[Unit]] {
        override def onError(ex: Throwable): Unit =
          logger.error(s"An exception occurred during consuming messages. cause: ${ex.getMessage}",
                       ex)
        override def onSuccess(value: List[Unit]): Unit = ()
      })
  }

  private def giveup(start: Instant): Boolean =
    Instant.now(clock).minusNanos(timeout.toNanos).compareTo(start) > 0

  def close(): Unit = connection.close()
}
