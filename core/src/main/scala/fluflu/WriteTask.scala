package fluflu

import java.util.concurrent.ExecutorService

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

  def apply[A](event: Event[A])(implicit encoder: Encoder[A]): Task[Int] = sender write (Message pack event)

  def close() = sender.close()

  override def finalize(): Unit = {
    super.finalize()
    close()
  }

}
