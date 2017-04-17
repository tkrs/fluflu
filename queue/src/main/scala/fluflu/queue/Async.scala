package fluflu
package queue

import java.time.Duration
import java.util.concurrent._
import java.util.concurrent.atomic.AtomicBoolean

import cats.syntax.either._
import com.typesafe.scalalogging.LazyLogging
import io.circe.Encoder

import scala.compat.java8.FunctionConverters._

final case class Async(
    messenger: Messenger,
    initialDelay: Duration = Duration.ofMillis(1),
    delay: Duration = Duration.ofSeconds(1),
    terminationDelay: Duration = Duration.ofSeconds(10)
) extends LazyLogging {

  private[this] val msgQueue: BlockingQueue[() => Either[Throwable, Letter]] = new LinkedBlockingQueue()
  private[this] val scheduler = Executors.newSingleThreadScheduledExecutor()

  private[this] val consume: (() => Either[Throwable, Letter]) => Unit = { fn =>
    msgQueue.remove(fn)
    fn()
      .leftMap(logger.error(s"Failed to encode a message to MessagePack", _))
      .flatMap(messenger.write).fold(
        e => logger.error(s"Failed to send a message to remote: ${messenger.host}:${messenger.port}", e),
        _ => ()
      )
  }

  def remaining: Int = msgQueue.size

  private def emit(): Unit = synchronized {
    logger.debug("Start emitting.")
    val start = System.nanoTime()
    msgQueue.stream.forEach(asJavaConsumer(consume))
    logger.debug(s"A emitting spend ${TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - start)} ms.")
  }

  def emit[A: Encoder](e: Event[A]): Either[Exception, Unit] =
    if (msgQueue offer (() => Messages.pack(e).map(Letter))) Either.right(R.start())
    else Either.left(new Exception("A queue no space is currently available"))

  def close(): Unit = {
    scheduler.shutdown()
    scheduler.awaitTermination(terminationDelay.toNanos, TimeUnit.NANOSECONDS)
    if (!scheduler.isTerminated) scheduler.shutdownNow()
    if (!msgQueue.isEmpty) logger.debug(s"A message queue has remaining: ${msgQueue.size()}")
    emit()
    messenger.close()
  }

  object R extends Runnable {
    private[this] val running: AtomicBoolean = new AtomicBoolean(false)
    def start(): Unit =
      if (!running.get() && running.compareAndSet(false, true))
        scheduler.execute(this)
    override def run(): Unit = if (!msgQueue.isEmpty) {
      emit()
      running.set(false)
      if (!scheduler.isShutdown)
        scheduler.schedule(new Runnable() {
          override def run(): Unit = scheduler.execute(this)
        }, delay.toNanos, TimeUnit.NANOSECONDS)
    }
  }
}
