import java.net.InetSocketAddress
import java.nio.ByteBuffer
import java.time.{ Clock, Duration }
import java.util.concurrent._

import fluflu.connection.NioConnection
import fluflu.queue.Async

/**
 * usage: sbt "examples/runMain NioExample ${host} ${port} ${total processing time} ${request per second}"
 * example: sbt "examples/runMain NioExample 127.0.0.1 24224 60 2000"
 */
object NioExample extends Example[ByteBuffer] {

  implicit val clock: Clock = Clock.systemUTC()

  implicit val connection = NioConnection(
    remote = new InetSocketAddress(args(0), args(1).toInt),
    reconnectionTimeout = Duration.ofSeconds(10),
    reconnectionBackoff = reconnectionBackoff,
    rewriteTimeout = Duration.ofSeconds(10),
    rewriteBackoff = rewriteBackoff
  )

  override val asyncQueue: Async[ByteBuffer] = Async(
    initialBufferSize = 2048,
    initialDelay = 50,
    delay = 100,
    delayTimeUnit = TimeUnit.MILLISECONDS,
    terminationDelay = 16,
    terminationDelayTimeUnit = TimeUnit.SECONDS
  )

  run
}
