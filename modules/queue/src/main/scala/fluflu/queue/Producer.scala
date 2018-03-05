package fluflu
package queue

import java.time.Instant
import java.util
import java.util.concurrent.ScheduledExecutorService

import com.typesafe.scalalogging.LazyLogging
import fluflu.msgpack.Packer

final class Producer private[fluflu] (scheduler: ScheduledExecutorService,
                                      queue: util.Queue[() => Either[Throwable, Array[Byte]]])(
    implicit
    PS: Packer[String],
    PI: Packer[Instant])
    extends LazyLogging {

  def emit[A: Packer](tag: String, record: A, time: Instant): Either[Exception, Unit] =
    if (scheduler.isShutdown)
      Left(new Exception("A Client scheduler was already shutdown"))
    else {
      val fa = () => Packer[(String, A, Instant)].apply((tag, record, time))
      if (queue.offer(fa)) Right(())
      else Left(new Exception("A queue no space is currently available"))
    }
}
