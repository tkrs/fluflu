package fluflu.queue

import java.time.Duration

import fluflu.msgpack.Packer
import fluflu.time.integer._
import org.scalatest.BeforeAndAfterEach

class ClientSpec extends Suite with BeforeAndAfterEach {

  var client: Client = _

  override protected def beforeEach(): Unit = {
    client = Client(Duration.ofMillis(100), Duration.ofSeconds(10))
  }

  override protected def afterEach(): Unit = {
    client.close()
  }

  implicit val packer: Packer[Int] = new Packer[Int] {
    def apply(a: Int): Either[Throwable, Array[Byte]] = Right(Array(0x10))
  }

  describe("emit") {
    it("should emit successfully") {
      val x = client.emit("t.l", 10)
      assert(x.isRight)
    }
  }
}
