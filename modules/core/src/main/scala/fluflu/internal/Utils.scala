package fluflu.internal

import java.util.concurrent.{ExecutorService, ThreadFactory}

import scala.concurrent.duration.{FiniteDuration, NANOSECONDS}

private[fluflu] object Utils {
  def awaitTermination(pool: ExecutorService, delay: FiniteDuration): Unit = {
    pool.shutdown()
    try if (!pool.awaitTermination(delay.toNanos, NANOSECONDS)) {
      pool.shutdownNow()
      if (!pool.awaitTermination(delay.toNanos, NANOSECONDS))
        throw new Exception("Pool did not terminate")
    } catch {
      case _: InterruptedException =>
        pool.shutdownNow()
        Thread.currentThread().interrupt()
    }
  }

  def namedThreadFactory(name: String, isDaemon: Boolean = false): ThreadFactory = new ThreadFactory {
    def newThread(r: Runnable): Thread = {
      val t = new Thread(r)
      t.setName(name)
      t.setDaemon(isDaemon)
      if (t.getPriority != Thread.NORM_PRIORITY) t.setPriority(Thread.NORM_PRIORITY)
      t
    }
  }
}
