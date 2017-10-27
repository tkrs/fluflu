package fluflu

trait Messenger {

  type Elm = () => Either[Throwable, Array[Byte]]

  def emit(elms: Iterator[Elm]): Unit

  def close(): Unit
}

object Messenger {

  lazy val noop: Messenger = new Messenger {
    override def emit(elms: Iterator[Elm]): Unit = ()
    override def close(): Unit                   = ()
  }
}
