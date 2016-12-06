package fluflu
package queue

import java.time.{ Clock, Instant }
import java.util.concurrent._

import cats.syntax.either._
import com.typesafe.scalalogging.LazyLogging
import io.circe.Encoder

import scala.util.{ Either => \/ }
import scala.compat.java8.FunctionConverters._

final case class Async[P](
    initialBufferSize: Int = 1024,
    initialDelay: Long = 0,
    delay: Long = 1,
    delayTimeUnit: TimeUnit = TimeUnit.SECONDS,
    terminationDelay: Long = 10,
    terminationDelayTimeUnit: TimeUnit = TimeUnit.SECONDS
)(implicit clock: Clock, connection: Connection[P], P: Message[P]) extends LazyLogging {

  private[this] val letterQueue: BlockingDeque[() => Throwable \/ P] = new LinkedBlockingDeque()
  private[this] val scheduler = Executors.newScheduledThreadPool(1)

  private[this] val command: Runnable = new Runnable {

    private[this] val processLetter = { (letter: P) =>
      connection.write(letter, 0, Instant.now(clock)).fold(
        logger.error(s"Failed to send a message to remote: ${connection.remote.getHostName}:${connection.remote.getPort}", _),
        _ => ()
      )
    }

    override def run(): Unit = {
      val start = System.nanoTime()
      logger.debug(s"Schedule start: ${Instant.now()}")
      try {
        letterQueue.parallelStream().forEach(asJavaConsumer { fn =>
          letterQueue.remove(fn)
          fn().fold(logger.error("Failed to encode to the json (circe AST)", _), processLetter)
        })
      } finally {
        logger.debug(s"elapsed time: ${TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - start)}ms")
      }
    }
  }

  scheduler.scheduleWithFixedDelay(command, 0, delay, delayTimeUnit)

  def size: Int = letterQueue.size

  def push[A: Encoder](evt: Event[A]): Exception \/ Unit = {
    if (scheduler.isShutdown) return \/.left(new Exception("Already shutdown a worker"))
    if (letterQueue offer (() => P(evt))) \/.right(())
    else \/.left(new Exception("A queue no space is currently available"))
  }

  def close(): Unit = {
    scheduler.shutdown()
    scheduler.awaitTermination(terminationDelay, terminationDelayTimeUnit)
    if (!scheduler.isTerminated) scheduler.shutdownNow()
    if (!letterQueue.isEmpty) logger.debug(s"message queue has remaining: ${letterQueue.size()}")
    command.run()
    connection.close()
    logger.info("Shutdown successful")
  }
}
