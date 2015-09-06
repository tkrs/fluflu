package fluflu

import scalaz.concurrent._

object WriteActor {
  def apply(
    tagPrefix: String,
    bufferCapacity: Int = 1 * 1024 * 1024)(
      implicit sender: Sender,
      strategy: Strategy) =
    new WriteActor(tagPrefix, bufferCapacity)

}

class WriteActor(
    val tagPrefix: String,
    val bufferCapacity: Int)(
        implicit sender: Sender,
        strategy: Strategy) {

  import Actor._

  private[this] def act[A](
    implicit dec: RecordDecoder[A],
    onError: Throwable => Unit): Actor[Event[A]] =
    actor(
      { msg =>
        val buf = Utils.createBuffer(tagPrefix, bufferCapacity, msg)
        sender.write(buf)
      }, onError
    )

  def apply[A](a: Event[A])(
    implicit dec: RecordDecoder[A],
    onError: Throwable => Unit): Unit = this ! a

  def contramap[B, A](f: Event[B] => Event[A])(
    implicit dec: RecordDecoder[A],
    onError: Throwable => Unit): Actor[Event[B]] = new Actor[Event[B]](b => this ! f(b), onError)(strategy)

  def ![A](evt: Event[A])(
    implicit dec: RecordDecoder[A],
    onError: Throwable => Unit): Unit = act ! evt

  def close() = sender.close()

  override def finalize(): Unit = {
    super.finalize()
    close()
  }

}

object WriteActorFunc {
  implicit val DefaultErrorHandle: Throwable => Unit = { e =>
    e.printStackTrace()
    throw e
  }
}
