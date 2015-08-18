# fluflu
fluent logger for scala

[![wercker status](https://app.wercker.com/status/d754e7976e64af6e1065568b43b27ac7/m "wercker status")](https://app.wercker.com/project/bykey/d754e7976e64af6e1065568b43b27ac7)

[![codecov.io](http://codecov.io/github/tkrs/fluflu/coverage.svg?branch=master)](http://codecov.io/github/tkrs/fluflu?branch=master)

## Usage

Add to your `build.sbt`

```scala
libraryDependencies += "com.github.tkrs" %% "fluflu" % "0.1.0-SNAPSHOT"
libraryDependencies += "io.argonaut" %% "argonaut" % "6.1"
libraryDependencies += "com.github.xuwei-k" % "msgpack4z-api" % "0.1.0"
libraryDependencies += "com.github.xuwei-k" %% "msgpack4z-core" % "0.1.4"
libraryDependencies += "com.github.xuwei-k" %% "msgpack4z-native" % "0.1.1"
libraryDependencies += "com.github.xuwei-k" %% "msgpack4z-argonaut" % "0.1.3"
```

In below example we use the [argonaut-shapeless](https://github.com/alexarchambault/argonaut-shapeless).

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
t attempt event // Task[Throwable \/ Long]
```

## TODO

- TEST

## COPYRIGHT

Copyright (c) 2015 Takeru Sato

## LICENSE

MIT
