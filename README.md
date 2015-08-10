# fluflu
fluent logger for scala

```scala
import fluflu._
import argonaut._, Argonaut._, Shapeless._
import scalaz.concurrent.Strategy.DefaultStrategy
case class Person(name: String, age: Int)
implicitly[EncodeJson[Person]]
implicitly[DecodeJson[Person]]
implicit def record2Json(p: Person): Json = p.asJson
val w = WriteActor[Person]()
val person = Person("Takeru", 31)
w ! Event("label.xxx", person)
```