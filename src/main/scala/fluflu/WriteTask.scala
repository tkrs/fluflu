package fluflu

import java.util.concurrent.{ ExecutorService }
import scalaz.\/
import scalaz.concurrent.{ Task, Strategy }

object WriteTask {
  def apply(
    tagPrefix: String,
    bufferCapacity: Int = 1 * 1024 * 1024
  )(
    implicit
    sender: Sender,
    strategy: ExecutorService = Strategy.DefaultExecutorService
  ) =
    new WriteTask(tagPrefix, bufferCapacity)

}

class WriteTask(
    val tagPrefix: String,
    val bufferCapacity: Int
)(
    implicit
    sender: Sender,
    strategy: ExecutorService
) {

  private[this] def write[A](event: Event[A])(implicit decoder: RecordDecoder[A]) = {
    val buf = Utils.createBuffer(tagPrefix, bufferCapacity, event)
    sender.write(buf)
  }

  def apply[A](event: Event[A])(implicit decoder: RecordDecoder[A]) = Task { this write event }

  def runAsync[A](event: Event[A])(f: \/[Throwable, Int] => Unit)(implicit decoder: RecordDecoder[A]) = Task {
    this write event
  } runAsync f

  def run[A](event: Event[A])(implicit decoder: RecordDecoder[A]) = Task {
    this write event
  } run

  def attempt[A](event: Event[A])(implicit decoder: RecordDecoder[A]) = Task {
    this write event
  } attempt

  def attemptRun[A](event: Event[A])(implicit decoder: RecordDecoder[A]) = this attempt (event) run

  def close() = sender.close()

  override def finalize(): Unit = {
    super.finalize()
    this.close()
  }

}
