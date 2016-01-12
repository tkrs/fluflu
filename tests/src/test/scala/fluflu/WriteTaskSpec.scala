package fluflu

import java.io.IOException

import org.scalatest._
import scalaz.concurrent.Task

import data.Event

class WriteTaskSpec extends FlatSpec with Matchers {

  object Z {

    implicit val defaultSender = new Sender {
      override def write(b: Array[Byte]): Task[Int] = Task.now(45)
      override def close(): Unit = {}
    }

    implicit val errorSender = new Sender {
      override def write(b: Array[Byte]): Task[Int] = Task(throw new IOException())
      override def close(): Unit = {}
    }

  }

  it should "return buffer size which successful write buffer" in {

    import io.circe.generic.auto._

    import Z.defaultSender

    case class Person(name: String, age: Int)

    val wt = WriteTask()

    val person = Person("tkrs", 31)
    val event = Event("tag-prefix", "label.test", person)

    {
      val ret = wt(event).run
      ret shouldEqual 45
    }

  }

  "WriteTaskTest#attemptRun" should "return left value if it is failed write buffer" in {

    import io.circe.generic.auto._

    import Z.errorSender

    case class Person(name: String, age: Int)

    val wt = WriteTask()

    val person = Person("tkrs", 31)
    val event = Event("tag-prefix", "label.test", person)

    val t = wt(event).attemptRun
    assert(t isLeft)
  }
}
