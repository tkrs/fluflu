package fluflu

import java.io.IOException
import java.nio.ByteBuffer

import org.scalatest._

class WriteTaskSpec extends FlatSpec with Matchers {

  object Z {

    implicit val defaultSender = new Sender {
      override def write(b: ByteBuffer): Int = 45
      override def close(): Unit = {}
    }

    implicit val errorSender = new Sender {
      override def write(b: ByteBuffer): Int = throw new IOException()
      override def close(): Unit = {}
    }

  }

  it should "return buffer size which successful write buffer" in {

    import argonaut._, Argonaut._, Shapeless._

    import Z.defaultSender

    case class Person(name: String, age: Int)

    implicitly[EncodeJson[Person]]
    implicitly[DecodeJson[Person]]

    import scalaz.concurrent.Strategy.DefaultStrategy
    implicit val personDecoder = RecordDecoder[Person](_.asJson)

    val wt = WriteTask("tag-prefix")

    val person = Person("tkrs", 31)
    val event = Event("label.test", person)

    {
      val ret = wt(event).run
      ret shouldEqual 45
    }

    {
      wt.runAsync(event) { x => x shouldEqual 45 }
    }

    {
      val ret = wt run event
      ret shouldEqual 45
    }

    {
      val ret = wt attemptRun event
      ret.getOrElse(0L) shouldEqual 45
    }
  }

  "WriteTaskTest#attemptRun" should "return left value if it is failed write buffer" in {

    import argonaut._, Argonaut._, Shapeless._

    import Z.errorSender

    case class Person(name: String, age: Int)

    implicitly[EncodeJson[Person]]
    implicitly[DecodeJson[Person]]

    import scalaz.concurrent.Strategy.DefaultStrategy
    implicit val personDecoder = RecordDecoder[Person](_.asJson)

    val wt = WriteTask("tag-prefix")

    val person = Person("tkrs", 31)
    val event = Event("label.test", person)

    val t = wt attemptRun event
    assert(t isLeft)
  }
}
