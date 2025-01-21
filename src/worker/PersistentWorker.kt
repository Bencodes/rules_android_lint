package com.rules.android.lint.worker

import com.google.devtools.build.lib.worker.ProtoWorkerMessageProcessor
import com.google.devtools.build.lib.worker.WorkRequestHandler
import java.io.IOException
import java.io.PrintStream
import java.time.Duration

internal class PersistentWorker(
  private val workerWorkRequestCallback: Worker.WorkRequestCallback,
) : Worker {
  override fun processRequests(): Int {
    val realStdErr: PrintStream = System.err

    try {
      val workerHandler: WorkRequestHandler =
        WorkRequestHandler
          .WorkRequestHandlerBuilder(
            WorkRequestHandler.WorkRequestCallback { request, pw ->
              return@WorkRequestCallback workerWorkRequestCallback.processWorkRequest(
                request.argumentsList.toList(),
                System.err,
              )
            },
            realStdErr,
            ProtoWorkerMessageProcessor(System.`in`, System.out),
          ).setCpuUsageBeforeGc(Duration.ofSeconds(10))
          .build()
      workerHandler.processRequests()
    } catch (e: IOException) {
      e.printStackTrace(realStdErr)
      return 1
    }

    return 0
  }
}
