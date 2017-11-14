package fluflu.monix

import java.io.IOException
import java.nio.ByteBuffer
import java.time.{Clock, Duration, Instant}

import com.typesafe.scalalogging.LazyLogging
import fluflu.{Backoff, Connection, Messenger}
import _root_.monix.eval.{Callback, Task}
import _root_.monix.execution.Scheduler
import monix.reactive.{Consumer, Observable}

import scala.concurrent.duration._

final class MessengerStream(timeout: Duration, backoff: Backoff)(implicit connection: Connection,
                                                                 taskScheduler: Scheduler,
                                                                 clock: Clock)
    extends Messenger
    with LazyLogging {

  private def go(buffer: ByteBuffer, retries: Int, start: Instant): Task[Unit] =
    Task
      .fromTry(connection.write(buffer))
      .flatMap { _ =>
        if (!buffer.hasRemaining) Task.unit
        else go(buffer, retries, start)
      }
      .onErrorRecoverWith {
        case e: IOException =>
          buffer.flip()
          if (giveup(start)) Task.raiseError(e)
          else
            go(buffer, retries + 1, start)
              .delayExecution(backoff.nextDelay(retries).toNanos.nanos)
      }

  private[this] val consumer = Consumer.foreachTask[Elm] { f =>
    f() match {
      case Left(e) =>
        logger.warn(e.getMessage); Task.unit
      case Right(arr) =>
        go(ByteBuffer.wrap(arr), 0, Instant.now(clock))
    }
  }

  def emit(elms: Iterator[Elm]): Unit = {
    val observer = Observable.fromIterator(elms)
    consumer(observer).runAsync(new Callback[Unit] {
      override def onError(ex: Throwable): Unit =
        logger.error(s"An exception occurred during consuming messages. Cause: ${ex.getMessage}",
                     ex)
      override def onSuccess(value: Unit): Unit = ()
    })
  }

  private def giveup(start: Instant): Boolean =
    Instant.now(clock).minusNanos(timeout.toNanos).compareTo(start) > 0

  def close(): Unit = connection.close().get
}
