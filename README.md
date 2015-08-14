# fluflu
fluent logger for scala

```scala
import fluflu._
import argonaut._, Argonaut._, Shapeless._

case class Person(name: String, age: Int)

implicitly[EncodeJson[Person]]
implicitly[DecodeJson[Person]]

import scalaz.concurrent.Strategy.DefaultStrategy
implicit val sender = DefaultSender()
implicit val personDecoder = RecordDecoder[Person] { person =>
  person.asJson
}
import fluflu.WriteActorFunc.DefaultErrorHandle

val person = Person("tkrs", 31)
val event = Event("label.xxx", person)

// Actor case
val a = WriteActor[Person]("tag-prefix")
a ! event

// Task case

val t = WriteTask[Person]("tag-prefix")
t(event) // => Task[Event[Person]]
t run event // => Long
t attempt event // Taks[Throwable \/ Long]
```

## TODO

- TEST
