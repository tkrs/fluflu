# fluflu
fluent logger for scala

[![wercker status](https://app.wercker.com/status/d754e7976e64af6e1065568b43b27ac7/m "wercker status")](https://app.wercker.com/project/bykey/d754e7976e64af6e1065568b43b27ac7)

[![codecov.io](http://codecov.io/github/tkrs/fluflu/coverage.svg?branch=master)](http://codecov.io/github/tkrs/fluflu?branch=master)

## Usage

Add to your `build.sbt`

```scala
libraryDependencies ++= Seq(
  "com.github.tkrs" %% "fluflu-core" % "0.2.1",
  "com.github.tkrs" %% "fluflu-msgpack" % "0.2.1"
)
```

```scala
import fluflu._
import io.circe.generic.auto._

case class Person(name: String, age: Int)

import scalaz.concurrent.Strategy.DefaultStrategy
implicit val sender = DefaultSender()

val person = Person("tkrs", 99)
val event = Event("prefix", "person", person)

val t = WriteTask()
t(event) // => Task[Event[Person]]
```

## TODO

- TEST

## COPYRIGHT

Copyright (c) 2015 Takeru Sato

## LICENSE

MIT
