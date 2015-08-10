package fluflu

final case class Event[A](
  label: String,
  record: A,
  time: Long = System.currentTimeMillis / 1000
)
