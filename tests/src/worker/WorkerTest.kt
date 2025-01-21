package com.rules.android.lint.worker

import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import java.io.PrintStream

@RunWith(JUnit4::class)
class WorkerTest {
  @Test
  fun `does return persistent worker when persistent worker flags are passed`() {
    val worker = Worker.fromArgs(arrayOf("--persistent_worker"), NO_OP_CALLBACK)
    assertThat(worker).isInstanceOf(PersistentWorker::class.java)
  }

  @Test
  fun `does return invocation worker when persistent worker flags are not passed`() {
    val worker = Worker.fromArgs(emptyArray(), NO_OP_CALLBACK)
    assertThat(worker).isInstanceOf(InvocationWorker::class.java)
  }

  @Test
  fun `does invocation worker return expected exit code`() {
    val worker = Worker.fromArgs(arrayOf(""), stubWorkRequestCallback({ 100 }))
    val exitCode = worker.processRequests()
    assertThat(exitCode).isEqualTo(100)
  }

  companion object {
    fun stubWorkRequestCallback(callback: () -> Int): Worker.WorkRequestCallback =
      object : Worker.WorkRequestCallback {
        override fun processWorkRequest(
          args: List<String>,
          printStream: PrintStream,
        ): Int = callback()
      }

    private val NO_OP_CALLBACK =
      stubWorkRequestCallback(
        callback = { TODO("NO-OP!") },
      )
  }
}
