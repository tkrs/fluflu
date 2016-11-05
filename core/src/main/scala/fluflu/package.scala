import java.nio.ByteBuffer

package object fluflu

package fluflu {

  final case class Letter(message: ByteBuffer, retries: Int)
}
