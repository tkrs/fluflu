package fluflu

import scalaz.concurrent._

object WriteActor {
  def apply[A](
    tagPrefix: String,
    bufferCapacity: Int = 1 * 1024 * 1024
  )(implicit sender: Sender, strategy: Strategy, decoder: RecordDecoder[A], onError: Throwable => Unit) =
    new WriteActor[A](tagPrefix, bufferCapacity)

  implicit def DefaultErrorHandle(e: Throwable): Unit = {
    e.printStackTrace()
    throw e
  }
}

class WriteActor[A](
    val tagPrefix: String,
    val bufferCapacity: Int
)(implicit sender: Sender, strategy: Strategy, decoder: RecordDecoder[A], onError: Throwable => Unit) {

  import Actor._

  private[this] val act: Actor[Event[A]] =
    actor(
      { msg =>
        val buf = Utils.createBuffer(tagPrefix, bufferCapacity, msg)
        sender.write(buf)
      }, onError
    )

  def apply(a: Event[A]): Unit = this ! a

  def contramap[B](f: B => Event[A]): Actor[B] = new Actor[B](b => this ! f(b), onError)(strategy)

  def !(evt: Event[A]): Unit = act ! evt

  def ?[B](evt: Event[A]): Task[B] = ???

  def close() = sender.close()

  override def finalize(): Unit = {
    super.finalize()
    close()
  }

}
