package com.android.tools.lint

import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.CyclicBarrier
import java.util.concurrent.TimeUnit

/**
 * Test stub standing in for lint's real entrypoint. [AndroidLintCliInvoker] resolves this class
 * reflectively through the jar classloader, which delegates to the test classpath, so tests can
 * observe invocations through the companion recorders.
 */
class Main {
  @JvmField
  val flags: LintCliFlags = LintCliFlags()

  fun run(args: Array<String>): Int {
    runBarrier?.await(5, TimeUnit.SECONDS)
    recordedRuns.add(RunRecord(this, args.toList(), flags.checkDependencies))
    return exitCode
  }

  data class RunRecord(
    val instance: Main,
    val args: List<String>,
    val checkDependencies: Boolean,
  )

  companion object {
    val recordedRuns = CopyOnWriteArrayList<RunRecord>()

    @Volatile
    var exitCode: Int = 0

    /** When set, run() blocks until [CyclicBarrier.parties] runs are in flight concurrently. */
    @Volatile
    var runBarrier: CyclicBarrier? = null

    fun reset() {
      recordedRuns.clear()
      exitCode = 0
      runBarrier = null
    }
  }
}
