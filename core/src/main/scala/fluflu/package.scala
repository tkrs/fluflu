import java.nio.ByteBuffer

package object fluflu

package fluflu {

  final case class Event[A](
    prefix: String,
    label: String,
    record: A,
    time: Long = System.currentTimeMillis / 1000
  )

  final case class Letter(message: ByteBuffer) extends AnyVal
}
