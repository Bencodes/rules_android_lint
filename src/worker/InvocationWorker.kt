package com.rules.android.lint.worker

internal class InvocationWorker(
  private val args: Array<String>,
  private val workerMessageProcessor: Worker.WorkRequestCallback,
) : Worker {
  override fun processRequests(): Int {
    // Handle a single work request
    return workerMessageProcessor.processWorkRequest(args.toList(), System.err)
  }
}
