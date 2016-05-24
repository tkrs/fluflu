import java.util.concurrent.{ ForkJoinPool, TimeUnit }
import java.util.concurrent.ForkJoinPool.ManagedBlocker

package object fluflu {
  private[fluflu] def blocker(duration: Long): Unit =
    ForkJoinPool.managedBlock(new ManagedBlocker {
      val started = System.currentTimeMillis()
      override def isReleasable: Boolean =
        System.currentTimeMillis() - started > duration
      override def block(): Boolean =
        if (isReleasable) true else {
          TimeUnit.MILLISECONDS.sleep(10)
          isReleasable
        }
    })
}
