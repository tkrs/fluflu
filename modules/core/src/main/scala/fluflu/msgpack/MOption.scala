package fluflu.msgpack

final case class MOption(
  chunk: Option[String] = None,
  size: Option[Int] = None,
  compressed: Option[String] = None
)
