package fluflu.connection

import java.net.InetSocketAddress
import java.time.{ Clock, Duration }

import cats.syntax.either._
import fluflu.{ Backoff, Connection }
import io.netty.bootstrap.Bootstrap
import io.netty.channel._
import io.netty.channel.nio.NioEventLoopGroup
import io.netty.channel.socket.SocketChannel
import io.netty.channel.socket.nio.NioSocketChannel

import scala.concurrent.Promise
import scala.util.{ Either => \/ }

final case class NettyConnection[A](
  remote: InetSocketAddress,
  reconnectionTimeout: Duration,
  reconnectionBackoff: Backoff,
  rewriteTimeout: Duration,
  rewriteBackoff: Backoff
)(implicit I: ChannelInitializer[SocketChannel], clock: Clock)
    extends Connection[A](rewriteTimeout, rewriteBackoff) {

  private[this] val group = new NioEventLoopGroup()

  private[this] val bootstrap = new Bootstrap()
    .group(group)
    .channel(classOf[NioSocketChannel])
    .option[java.lang.Boolean](ChannelOption.TCP_NODELAY, true)
    .handler(I)

  private[this] val f = bootstrap.connect(remote).sync()

  def write(message: A): Throwable \/ Unit = {
    val promise = Promise[Unit]
    f.channel()
      .writeAndFlush(message)
      .addListener(new ChannelFutureListener {
        override def operationComplete(future: ChannelFuture): Unit = {
          val ex = Option(future.cause())
          if (ex.isDefined) {
            promise.failure(ex.get)
          } else {
            promise.success(())
          }
        }
      })
    // promise.future
    \/.right(())
  }

  def close(): Unit = {
    f.channel()
      .closeFuture()
      .addListener(new ChannelFutureListener {
        override def operationComplete(future: ChannelFuture): Unit = {
          // noop
        }
      })
    group.shutdownGracefully()
    group.terminationFuture().sync()
  }
}
