package fluflu

case class Event[A](
  tag: String,
  time: Long = System.currentTimeMillis,
  record: A
)
