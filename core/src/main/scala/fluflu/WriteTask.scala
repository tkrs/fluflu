package fluflu

import java.util.concurrent.ExecutorService

import cats.data.Xor

import scalaz.concurrent.{ Task, Strategy }

object WriteTask {
  def apply()(
    implicit
    sender: Sender,
    strategy: ExecutorService = Strategy DefaultExecutorService
  ) = {
    new WriteTask()
  }

}

class WriteTask()(
    implicit
    sender: Sender,
    strategy: ExecutorService
) {

  import data.Event
  import io.circe.Encoder

  // TODO: Want to be break up with scalaz.Task
  def apply[A](event: Event[A])(implicit encoder: Encoder[A]): Task[Int] =
    Message pack event match {
      case Xor.Left(e) => Task fail e
      case Xor.Right(v) => sender write v
    }

  def close() = sender.close()

  override def finalize(): Unit = {
    super.finalize()
    close()
  }

}
