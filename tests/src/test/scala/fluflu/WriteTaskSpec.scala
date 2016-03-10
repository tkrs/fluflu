package fluflu

import java.io.IOException

import cats.MonadError
import org.scalatest._
import data.Event

import scala.concurrent.{ Await, Future }
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.Duration

class WriteTaskSpec extends FlatSpec with Matchers {

  object Z {

    implicit val defaultSender = new Sender {
      override def write(b: Array[Byte]): Future[Int] = Future(45)
      override def close(): Unit = {}
    }

    implicit val errorSender = new Sender {
      override def write(b: Array[Byte]): Future[Int] = Future(throw new IOException())
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
      val ret = Await.result(wt(event), Duration.Inf)
      ret shouldEqual 45
    }

  }

  "WriteTaskTest#attemptRun" should "return left value if it is failed write buffer" in {

    import cats.std.future._
    import io.circe.generic.auto._

    import Z.errorSender
    val MF = MonadError[Future, Throwable]

    case class Person(name: String, age: Int)

    val wt = WriteTask()

    val person = Person("tkrs", 31)
    val event = Event("tag-prefix", "label.test", person)

    val t = Await.result(MF.attempt(wt(event)), Duration.Inf)
    assert(t isLeft)
  }
}
