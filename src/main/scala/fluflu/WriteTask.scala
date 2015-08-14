package fluflu

import scalaz.\/
import scalaz.concurrent.{ Task, Strategy }

object WriteTask {
  def apply[A](
    tagPrefix: String,
    bufferCapacity: Int = 1 * 1024 * 1024
  )(
    implicit
    sender: Sender,
    strategy: Strategy,
    decoder: RecordDecoder[A]
  ) =
    new WriteTask[A](tagPrefix, bufferCapacity)

}

class WriteTask[A](
    val tagPrefix: String,
    val bufferCapacity: Int
)(
    implicit
    sender: Sender,
    strategy: Strategy,
    decoder: RecordDecoder[A]
) {

  private[this] def write(event: Event[A]) = {
    val buf = Utils.createBuffer(tagPrefix, bufferCapacity, event)
    sender.write(buf)
  }

  def apply(event: Event[A]) = Task { this write event }

  def runAsync(event: Event[A])(f: \/[Throwable, Long] => Unit) = Task {
    this write event
  } runAsync f

  def run(event: Event[A]) = Task {
    this write event
  } run

  def attempt(event: Event[A]) = Task {
    this write event
  } attempt

  def attemptRun(event: Event[A]) = this attempt (event) run

  def close() = sender.close()

  override def finalize(): Unit = {
    super.finalize()
    close()
  }

}
