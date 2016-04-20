package fluflu

import cats.data.Xor

import scala.concurrent.{ ExecutionContext, Future }

object WriteTask {
  def apply()(
    implicit
    s: Sender,
    ec: ExecutionContext
  ) = new WriteTask()
}

class WriteTask()(
    implicit
    s: Sender,
    ec: ExecutionContext
) {

  import data.Event
  import io.circe.Encoder

  def apply[A](event: Event[A])(implicit encoder: Encoder[A]): Future[Int] =
    Message pack event match {
      case Xor.Left(e) => Future.failed(e)
      case Xor.Right(bs) => s.write(bs)
    }

  def close() = s.close()

  override def finalize(): Unit = {
    super.finalize()
    close()
  }
}
