package fluflu.connection

import java.util

import cats.syntax.either._
import fluflu.msgpack.MessagePacker
import io.circe.Json
import io.netty.channel.{ ChannelHandlerContext, ChannelInitializer, ChannelOutboundHandlerAdapter, ChannelPromise }
import io.netty.channel.socket.SocketChannel
import io.netty.handler.codec.MessageToMessageEncoder

class NettySocketChannelInitializer extends ChannelInitializer[SocketChannel] {

  override def initChannel(ch: SocketChannel): Unit = {
    ch.pipeline().addLast(
      new CirceEncoder(),
      new ChannelOutboundHandlerAdapter {
        override def write(ctx: ChannelHandlerContext, msg: scala.Any, promise: ChannelPromise): Unit = {
          super.write(ctx, msg, promise)
        }
        override def close(ctx: ChannelHandlerContext, promise: ChannelPromise): Unit = {
          super.close(ctx, promise)
        }
      }
    )
  }
}

class CirceEncoder extends MessageToMessageEncoder[Json] {
  private[this] val packer = MessagePacker()
  override def encode(ctx: ChannelHandlerContext, msg: Json, out: util.List[AnyRef]): Unit = {
    packer.pack(msg).map { packed =>
      val alloc = ctx.alloc()
      val buf = alloc.buffer(packed.length)
      out.add(buf.writeBytes(packed))
    }
  }
}

