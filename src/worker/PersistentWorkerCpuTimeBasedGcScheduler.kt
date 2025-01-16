package com.rules.android.lint.worker

import com.sun.management.OperatingSystemMXBean
import java.lang.management.ManagementFactory
import java.time.Duration
import java.util.concurrent.atomic.AtomicReference

internal class PersistentWorkerCpuTimeBasedGcScheduler(
  /**
   * After this much CPU time has elapsed, we may force a GC run. Set to [Duration.ZERO] to
   * disable.
   */
  private val cpuUsageBeforeGc: Duration = Duration.ofSeconds(10),
) {
  private val cpuTime: Duration
    get() = if (!cpuUsageBeforeGc.isZero) Duration.ofNanos(bean.processCpuTime) else Duration.ZERO

  /** The total process CPU time at the last GC run (or from the start of the worker).  */
  private val cpuTimeAtLastGc: AtomicReference<Duration> = AtomicReference(cpuTime)

  /** Call occasionally to perform a GC if enough CPU time has been used.  */
  fun maybePerformGc() {
    if (!cpuUsageBeforeGc.isZero) {
      val currentCpuTime = cpuTime
      val lastCpuTime = cpuTimeAtLastGc.get()
      // Do GC when enough CPU time has been used, but only if nobody else beat us to it.
      if (currentCpuTime.minus(lastCpuTime) > cpuUsageBeforeGc &&
        cpuTimeAtLastGc.compareAndSet(lastCpuTime, currentCpuTime)
      ) {
        System.gc()
        // Avoid counting GC CPU time against CPU time before next GC.
        cpuTimeAtLastGc.compareAndSet(currentCpuTime, cpuTime)
      }
    }
  }

  companion object {
    /** Used to get the CPU time used by this process.  */
    private val bean = ManagementFactory.getOperatingSystemMXBean() as OperatingSystemMXBean
  }
}
