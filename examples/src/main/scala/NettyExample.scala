import java.net.InetSocketAddress
import java.time.{ Clock, Duration }
import java.util.concurrent._

import fluflu.connection.{ NettyConnection, NettySocketChannelInitializer }
import fluflu.queue.Async
import io.circe.Json

/**
 * usage: sbt "examples/runMain NettyExample ${host} ${port} ${total processing time} ${request per second}"
 * example: sbt "examples/runMain NettyExample 127.0.0.1 24224 60 2000"
 */
object NettyExample extends Example[Json] with App {

  implicit val clock: Clock = Clock.systemUTC()

  implicit val initializer = new NettySocketChannelInitializer

  implicit val connection = NettyConnection[Json](
    remote = new InetSocketAddress(args(0), args(1).toInt),
    reconnectionTimeout = Duration.ofSeconds(10),
    reconnectionBackoff = reconnectionBackoff,
    rewriteTimeout = Duration.ofSeconds(10),
    rewriteBackoff = rewriteBackoff
  )

  override val asyncQueue: Async[Json] = Async(
    initialBufferSize = 2048,
    initialDelay = 50,
    delay = 100,
    delayTimeUnit = TimeUnit.MILLISECONDS,
    terminationDelay = 16,
    terminationDelayTimeUnit = TimeUnit.SECONDS
  )

  run
}
