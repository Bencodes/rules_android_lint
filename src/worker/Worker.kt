package com.rules.android.lint.worker

import java.io.IOException
import java.io.PrintStream

interface Worker {

  fun processRequests(): Int

  interface WorkRequestCallback {

    /**
     * Processes an individual work request.
     */
    fun processWorkRequest(args: List<String>, printStream: PrintStream): Int
  }

  interface WorkerMessageProcessor {

    @Throws(IOException::class)
    fun readWorkRequest(): WorkRequest

    @Throws(IOException::class)
    fun writeWorkResponse(workResponse: WorkResponse)
  }

  companion object {

    /**
     * Creates the appropriate worker instance using the provided worker arguments.
     *
     * If `--persistent_worker` exists in the arguments, an instance of PersistentWorker will
     * be returned. Otherwise an instance of InvocationWorker will be returned.
     */
    fun fromArgs(
      args: Array<String>,
      workerMessageProcessor: WorkRequestCallback,
    ): Worker {
      return when {
        "--persistent_worker" in args -> PersistentWorker(workerMessageProcessor)
        else -> InvocationWorker(args, workerMessageProcessor)
      }
    }
  }
}
