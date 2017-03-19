package fluflu
package queue

import java.time.{ Clock, Duration }
import java.util.concurrent._

import cats.syntax.either._
import com.typesafe.scalalogging.LazyLogging
import io.circe.Encoder

import scala.util.{ Either => \/ }
import scala.compat.java8.FunctionConverters._

final case class Async(
    messenger: Messenger,
    initialDelay: Duration = Duration.ofMillis(1),
    delay: Duration = Duration.ofSeconds(1),
    terminationDelay: Duration = Duration.ofSeconds(10)
)(implicit clock: Clock) extends LazyLogging {

  private[this] val letterQueue: BlockingDeque[() => Throwable \/ Letter] = new LinkedBlockingDeque()
  private[this] val scheduler = Executors.newScheduledThreadPool(1)
  private[this] val consume: (() => Throwable \/ Letter) => Unit = { fn =>
    letterQueue.remove(fn)
    fn()
      .leftMap(logger.error(s"Failed to encode a message to Message-Pack", _))
      .flatMap(messenger.write).fold(
        e => logger.error(s"Failed to send a message to remote: ${messenger.host}:${messenger.port}", e),
        _ => ()
      )
  }

  private[this] val command: Runnable = new Runnable {
    override def run(): Unit =
      letterQueue.parallelStream().forEach(asJavaConsumer(consume))
  }

  private[this] val _: ScheduledFuture[_] =
    scheduler.scheduleWithFixedDelay(command, initialDelay.toNanos, delay.toNanos, TimeUnit.NANOSECONDS)

  def size: Int = letterQueue.size

  def push[A: Encoder](e: Event[A]): Exception \/ Unit =
    if (letterQueue offer (() => Messages.pack(e).map(Letter))) \/.right(())
    else \/.left(new Exception("A queue no space is currently available"))

  def close(): Unit = {
    scheduler.shutdown()
    scheduler.awaitTermination(terminationDelay.toNanos, TimeUnit.NANOSECONDS)
    if (!scheduler.isTerminated) scheduler.shutdownNow()
    if (!letterQueue.isEmpty) logger.debug(s"message queue has remaining: ${letterQueue.size()}")
    command.run()
    messenger.close()
  }
}