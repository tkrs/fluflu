package fluflu

import com.twitter.util.{ Future, FuturePool }

object WriteTask {
  def apply()(implicit s: Sender, p: FuturePool) = new WriteTask()
}

class WriteTask()(implicit s: Sender, p: FuturePool) {
  import data.Event
  import io.circe.Encoder

  def apply[A](event: Event[A])(implicit encoder: Encoder[A]): Future[Int] =
    Message pack event fold (Future.exception, bs => p(s.write(bs)))

  def close() = s.close()

  override def finalize(): Unit = {
    super.finalize()
    close()
  }
}
