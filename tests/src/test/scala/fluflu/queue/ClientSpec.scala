package fluflu.queue

import java.time.Duration

import cats.syntax.either._
import fluflu.Event
import org.scalatest.BeforeAndAfterEach

class ClientSpec extends Suite with BeforeAndAfterEach {

  import monix.execution.Scheduler.Implicits.global

  var client: Client = _

  override protected def beforeEach(): Unit = {
    client = Client(Duration.ofMillis(100), Duration.ofMillis(10))
  }

  override protected def afterEach(): Unit = {
    client.close()
  }

  describe("Client") {
    describe("emit") {
      it("should emit successfully") {
        val x = client.emit(Event.apply("t", "l", 10))
        x.toTry.get
      }
    }
  }

}
