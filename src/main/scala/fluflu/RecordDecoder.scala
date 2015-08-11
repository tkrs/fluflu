package fluflu

import argonaut._, Argonaut._

trait RecordDecoder[A] {
  def apply(a: A): Json
}

object RecordDecoder {
  def apply[A](f: A => Json): RecordDecoder[A] = new RecordDecoder[A] {
    def apply(a: A): Json = f(a)
  }
}
