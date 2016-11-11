package fluflu.queue

import java.nio.ByteBuffer
import java.time.{ Clock, Instant }
import java.util.concurrent.{ BlockingDeque, Executors, LinkedBlockingDeque, TimeUnit }

import cats.data.Xor
import fluflu.{ Event, Letter, Message, Messenger }
import io.circe.Encoder

import scala.annotation.tailrec
import scala.concurrent.{ ExecutionContext, Future, blocking }

final case class Writer(messenger: Messenger)(implicit clock: Clock) {

  private[this] val letterQueue: BlockingDeque[Letter] = new LinkedBlockingDeque[Letter]()
  private[this] val executor = Executors.newSingleThreadExecutor()

  private[this] val command: Runnable = new Runnable {
    private[this] val buffer = ByteBuffer.allocateDirect(1024)
    private[this] val blockingDuration: Long = 5000
    override def run(): Unit = {
      @tailrec def go(): Unit =
        if (executor.isShutdown) () else {
          Option(letterQueue.poll()) match {
            case None =>
              blocking(TimeUnit.NANOSECONDS.sleep(blockingDuration))
            case Some(letter) =>
              try {
                if (buffer.limit < letter.message.length) buffer.limit(letter.message.length)
                buffer.put(letter.message).flip()
                messenger.write(buffer, 0, Instant.now(clock))
              } finally {
                buffer.clear()
              }
          }
          go()
        }
      go()
    }
  }

  executor.execute(command)

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
