package fluflu
package msgpack

import java.time.Instant

import org.msgpack.core.MessagePacker

trait Packer[A] {
  def apply(a: A, packer: MessagePacker): Unit
}

object Packer {

  def apply[A](implicit P: Packer[A]): Packer[A] = P

  implicit val packString: Packer[String] = new Packer[String] {
    def apply(a: String, packer: MessagePacker): Unit =
      packer.packString(a)
  }

  implicit def packEvent[A](implicit
                            S: Packer[String],
                            A: Packer[A],
                            T: Packer[Instant]): Packer[(String, A, Instant)] =
    new Packer[(String, A, Instant)] {
      def apply(v: (String, A, Instant), packer: MessagePacker): Unit = {
        val (s, a, t) = v
        packer.packArrayHeader(3)
        S(s, packer)
        T(t, packer)
        A(a, packer)
      }
    }

  implicit def packEntry[A](implicit
                            A: Packer[A],
                            T: Packer[Instant]): Packer[(A, Instant)] =
    new Packer[(A, Instant)] {
      def apply(v: (A, Instant), packer: MessagePacker): Unit = {
        val (a, t) = v
        packer.packArrayHeader(2)
        T(t, packer)
        A(a, packer)
      }
    }

  def formatUInt32(v: Long): Array[Byte] = {
    Array((v >>> 24).toByte, (v >>> 16).toByte, (v >>> 8).toByte, (v >>> 0).toByte)
  }
}
