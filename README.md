# fluflu
fluent logger for scala

[![wercker status](https://app.wercker.com/status/d754e7976e64af6e1065568b43b27ac7/m "wercker status")](https://app.wercker.com/project/bykey/d754e7976e64af6e1065568b43b27ac7)

[![codecov.io](http://codecov.io/github/tkrs/fluflu/coverage.svg?branch=master)](http://codecov.io/github/tkrs/fluflu?branch=master)

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
val a = WriteActor("tag-prefix")
a ! event

// Task case
val t = WriteTask("tag-prefix")
t(event) // => Task[Event[Person]]
t run event // => Long
t attempt event // Taks[Throwable \/ Long]
```

## TODO

- TEST
