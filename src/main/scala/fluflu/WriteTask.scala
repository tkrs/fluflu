package fluflu

import scalaz.concurrent.{ Task, Strategy }

class WriteTask[A](
    val tagPrefix: String,
    val bufferCapacity: Int
)(implicit sender: Sender, strategy: Strategy, decoder: RecordDecoder[A]) {

  def run(event: Event[A]) = this.attemptRun(event).getOrElse(-1L)

  def attemptRun(event: Event[A]) = this attempt (event) run

  def attempt(event: Event[A]) = Task {
    val buf = Utils.createBuffer(tagPrefix, bufferCapacity, event)
    sender.write(buf)
  } attempt

  def close() = sender.close()

  override def finalize(): Unit = {
    super.finalize()
    close()
  }

}
