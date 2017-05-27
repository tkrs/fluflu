import java.time.Duration
import java.util.concurrent.{ ExecutorService, TimeUnit }

package object fluflu {

  def awaitTermination(pool: ExecutorService, delay: Duration): Unit = {
    pool.shutdown()
    try {
      def term = pool.awaitTermination(delay.toNanos, TimeUnit.NANOSECONDS)
      if (!term) {
        pool.shutdownNow()
        if (!term) throw new Exception("Pool did not terminate")
      }
    } catch {
      case ie: InterruptedException =>
        pool.shutdownNow()
        Thread.currentThread().interrupt()
    }
  }
}

package fluflu {

  final case class Letter(message: Array[Byte]) extends AnyVal
}
