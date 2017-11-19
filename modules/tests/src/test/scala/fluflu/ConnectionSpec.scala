package fluflu

import java.io.IOException
import java.net.InetSocketAddress
import java.nio.ByteBuffer
import java.nio.channels.SocketChannel
import java.time.{Clock, Duration}

import org.scalatest.{FunSpec, Matchers}
import org.scalatest.mockito.MockitoSugar
import org.mockito.Mockito._
import org.mockito.invocation.InvocationOnMock
import org.mockito.stubbing.Answer

import scala.util.Failure

class ConnectionSpec extends FunSpec with MockitoSugar with Matchers {
  import Connection.ConnectionImpl

  implicit val clock: Clock = Clock.systemUTC()

  val address: InetSocketAddress = mock[InetSocketAddress]
  val duration: Duration         = Duration.ofSeconds(3)
  val backoff: Backoff           = Backoff.fix(Duration.ofMillis(1))

  describe("constructor") {
    it("should create instance successfully with retry some time") {
      val channelMock = mock[SocketChannel]
      when(channelMock.connect(address))
        .thenThrow(new IOException("ε≡≡ﾍ( ´Д`)ﾉ"))
        .thenReturn(true)
      doNothing().when(channelMock).close()
      when(channelMock.isConnected)
        .thenReturn(false)
        .thenReturn(true)
      final class TestConnection extends ConnectionImpl(address, duration, backoff) {
        override protected def channelOpen: SocketChannel =
          channelMock
      }
      new TestConnection
    }
    it("should throw IOException when it gives up on the connection retry") {
      val channelMock = mock[SocketChannel]
      when(channelMock.connect(address))
        .thenThrow(new IOException("ε≡≡ﾍ( ´Д`)ﾉ"))
      doNothing().when(channelMock).close()
      final class TestConnection extends ConnectionImpl(address, Duration.ofMillis(5), backoff) {
        override protected def channelOpen: SocketChannel =
          channelMock
      }
      assertThrows[IOException](new TestConnection)
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
      when(channelMock.isConnected)
        .thenReturn(false)
        .thenReturn(true)
      when(channelMock.connect(address)).thenReturn(true)
      when(channelMock.write(arg)).thenThrow(new IOException)

      final class TestConnection extends ConnectionImpl(address, duration, backoff) {
        override protected def channelOpen: SocketChannel = channelMock
      }
      val conn = new TestConnection
      assert(conn.write(arg).isFailure)
    }
    it("should return Failure with message: Already closed when it was closed") {
      val arg         = ByteBuffer.wrap(Array(1, 2, 3).map(_.toByte))
      val channelMock = mock[SocketChannel]
      when(channelMock.isConnected)
        .thenReturn(false)
        .thenReturn(true)
      when(channelMock.connect(address))
        .thenReturn(true)
      when(channelMock.write(arg))
        .thenThrow(new IOException)

      final class TestConnection extends ConnectionImpl(address, duration, backoff) {
        override protected def channelOpen: SocketChannel = channelMock
      }
      val conn = new TestConnection
      conn.close()
      val Failure(e) = conn.write(arg)
      assert(e.getMessage === "Already closed")
    }
    it("should return Failure with IOException when it SocketChannel.connect returns false") {
      val arg         = ByteBuffer.wrap(Array(1, 2, 3).map(_.toByte))
      val channelMock = mock[SocketChannel]
      when(channelMock.isConnected)
        .thenReturn(false)
        .thenReturn(true)
      when(channelMock.connect(address))
        .thenReturn(true)
        .thenReturn(false)

      when(address.toString).thenReturn("hahahahahaha")
      final class TestConnection extends ConnectionImpl(address, duration, backoff) {
        override protected def channelOpen: SocketChannel = channelMock
      }

      val conn = new TestConnection
      assertThrows[IOException](conn.write(arg).get)
    }
  }
  describe("close") {
    it("should close successfully") {
      val channelMock = mock[SocketChannel]
      when(channelMock.connect(address))
        .thenReturn(true)
      doNothing().when(channelMock).close()
      when(channelMock.isConnected)
        .thenReturn(true)
      final class TestConnection extends ConnectionImpl(address, duration, backoff) {
        override protected def channelOpen: SocketChannel =
          channelMock
      }
      val conn = new TestConnection
      conn.close()
      assert(conn.isClosed)
    }
  }
}
