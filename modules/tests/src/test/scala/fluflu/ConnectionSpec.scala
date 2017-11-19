package fluflu

import java.io.IOException
import java.net.InetSocketAddress
import java.nio.ByteBuffer
import java.nio.channels.SocketChannel
import java.time.{Clock, Duration}
import java.util.concurrent.LinkedBlockingDeque

import org.scalatest.{FunSpec, Matchers}
import org.scalatest.mockito.MockitoSugar
import org.mockito.Mockito._
import org.mockito.invocation.InvocationOnMock
import org.mockito.stubbing.Answer

class ConnectionSpec extends FunSpec with MockitoSugar with Matchers {
  import Connection.ConnectionImpl

  implicit val clock: Clock = Clock.systemUTC()

  val address: InetSocketAddress = mock[InetSocketAddress]
  val duration: Duration         = Duration.ofSeconds(1)
  val backoff: Backoff           = Backoff.fix(duration)

  describe("constructor") {
    it("should create instance successfully with retry some time") {
      val isConnectedAnswers = new LinkedBlockingDeque[Boolean]()
      isConnectedAnswers.add(false)
      isConnectedAnswers.add(false)
      isConnectedAnswers.add(true)
      val arg         = ByteBuffer.wrap(Array(1, 2, 3).map(_.toByte))
      val channelMock = mock[SocketChannel]
      when(channelMock.isConnected).thenAnswer(new Answer[Boolean] {
        override def answer(invocation: InvocationOnMock): Boolean =
          isConnectedAnswers.take()
      })
      when(channelMock.connect(address)).thenReturn(true)
      when(channelMock.write(arg)).thenAnswer(new Answer[Int] {
        override def answer(invocation: InvocationOnMock): Int = {
          val arr = Array.ofDim[Byte](1)
          arg.get(arr)
          1
        }
      })
      final class TestConnection extends ConnectionImpl(address, duration, backoff) {
        override protected def channelOpen: SocketChannel = channelMock
      }
      new TestConnection
    }
  }
  describe("write") {
    it("should write successfully") {
      val arg         = ByteBuffer.wrap(Array(1, 2, 3).map(_.toByte))
      val channelMock = mock[SocketChannel]
      when(channelMock.isConnected).thenReturn(true)
      when(channelMock.connect(address)).thenReturn(true)
      when(channelMock.write(arg)).thenAnswer(new Answer[Int] {
        override def answer(invocation: InvocationOnMock): Int = {
          val arr = Array.ofDim[Byte](1)
          arg.get(arr)
          1
        }
      })
      final class TestConnection extends ConnectionImpl(address, duration, backoff) {
        override protected def channelOpen: SocketChannel = channelMock
      }
      val conn = new TestConnection
      assert(conn.write(arg).isSuccess)
    }
    it("should write failed when it occurs IOException") {
      val arg         = ByteBuffer.wrap(Array(1, 2, 3).map(_.toByte))
      val channelMock = mock[SocketChannel]
      when(channelMock.isConnected).thenReturn(true)
      when(channelMock.connect(address)).thenReturn(true)
      when(channelMock.write(arg)).thenThrow(new IOException)

      final class TestConnection extends ConnectionImpl(address, duration, backoff) {
        override protected def channelOpen: SocketChannel = channelMock
      }
      val conn = new TestConnection
      assert(conn.write(arg).isFailure)
    }
  }
}
