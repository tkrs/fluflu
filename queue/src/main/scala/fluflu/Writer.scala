package fluflu

import java.time.{ Clock, Duration, Instant }
import java.util.concurrent.{ BlockingDeque, Executors, LinkedBlockingDeque, TimeUnit }

import cats.data.Xor
import io.circe.Encoder

import scala.annotation.tailrec
import scala.concurrent.{ blocking, ExecutionContext, Future }

final case class Writer(
    host: String = "127.0.0.1",
    port: Int = 24224,
    reconnectionTimeout: Duration,
    rewriteTimeout: Duration,
    reconnectionBackoff: Backoff,
    rewriteBackoff: Backoff
)(implicit clock: Clock) {

  private[this] val letterQueue: BlockingDeque[Letter] = new LinkedBlockingDeque[Letter]()

  private[this] val messenger = Messenger(
    host,
    port,
    reconnectionTimeout,
    rewriteTimeout,
    reconnectionBackoff,
    rewriteBackoff
  )

  private[this] val executor = Executors.newSingleThreadExecutor()
  executor.execute(new Runnable {
    private[this] val blockingDuration: Long = 5000
    override def run(): Unit = {
      @tailrec def go(): Unit =
        if (executor.isShutdown) () else {
          Option(letterQueue.poll()) match {
            case None =>
              blocking(TimeUnit.NANOSECONDS.sleep(blockingDuration))
            case Some(letter) =>
              messenger.write(letter, 0, Instant.now(clock))
          }
          go()
        }
      go()
    }
  })

  def die: Boolean = messenger.die

  def write[A: Encoder](e: Event[A]): Throwable Xor Unit =
    Message.pack(e).map(msg => letterQueue addLast Letter(msg))

  def writeFuture[A: Encoder](e: Event[A])(implicit ec: ExecutionContext): Future[Throwable Xor Unit] =
    Future(Message pack e) map (packed => packed map (msg => letterQueue addLast Letter(msg)))

  def close(): Unit = {
    executor.shutdown()
    executor.awaitTermination(10, TimeUnit.SECONDS)
    if (!executor.isTerminated) executor.shutdownNow()
    messenger.close()
  }
}
