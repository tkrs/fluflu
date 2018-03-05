package fluflu

trait Messenger {

  def emit(elms: Iterator[Array[Byte]]): Unit

  def close(): Unit
}

object Messenger {

  lazy val noop: Messenger = new Messenger {
    def emit(elms: Iterator[Array[Byte]]): Unit = ()
    def close(): Unit                           = ()
  }
}
