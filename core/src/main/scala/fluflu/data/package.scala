package fluflu.data

final case class Event[A](
  prefix: String,
  label: String,
  record: A,
  time: Long = System.currentTimeMillis / 1000
)
