package com.rules.android.lint.worker

import io.reactivex.rxjava3.core.BackpressureStrategy
import io.reactivex.rxjava3.core.Flowable
import io.reactivex.rxjava3.core.Scheduler
import io.reactivex.rxjava3.schedulers.Schedulers
import java.io.BufferedOutputStream
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.PrintStream

internal class PersistentWorker(
  /**
   * WorkerIO instance wrapping the standard output streams
   */
  private val workerIO: WorkerIO,
  /**
   * Rxjava Scheduler to execute work requests on.
   */
  private val scheduler: Scheduler,
  /**
   * Instance of CpuTimeBasedGcScheduler that will run periodically
   */
  private val persistentWorkerCpuTimeBasedGcScheduler: PersistentWorkerCpuTimeBasedGcScheduler,
  /**
   * Instance of CpuTimeBasedGcScheduler that will run periodically
   */
  private val workRequestProcessor: Worker.WorkerMessageProcessor,
  /**
   * Instance of CpuTimeBasedGcScheduler that will run periodically
   */
  private val workerWorkRequestCallback: Worker.WorkRequestCallback,
) : Worker {
  constructor(
    workerMessageProcessor: Worker.WorkRequestCallback,
  ) : this(
    workerIO = WorkerIO(),
    scheduler = Schedulers.io(),
    persistentWorkerCpuTimeBasedGcScheduler = PersistentWorkerCpuTimeBasedGcScheduler(),
    workRequestProcessor =
      WorkerJsonMessageProcessor(
        System.`in`,
        System.out,
      ),
    workerWorkRequestCallback = workerMessageProcessor,
  )

  /**
   * Initiate the worker and begin processing work requests
   */
  override fun processRequests(): Int {
    return workerIO.use { io ->
      // Start by redirecting the system streams so that nothing
      // corrupts the streams that the worker uses
      io.redirectSystemStreams()

      // Process requests as they come in using RxJava
      Flowable
        .create(
          { emitter ->
            while (!emitter.isCancelled) {
              try {
                val request: WorkRequest = workRequestProcessor.readWorkRequest()
                emitter.onNext(request)
              } catch (e: IOException) {
                emitter.onError(e)
              }
            }
          },
          BackpressureStrategy.BUFFER,
        ).subscribeOn(scheduler)
        .parallel()
        .runOn(scheduler)
        // Execute the work and map the result to a work response
        .map { request -> return@map this.respondToRequest(request) }
        // Run the garbage collector periodically so that we are a good responsible worker
        .doOnNext { persistentWorkerCpuTimeBasedGcScheduler.maybePerformGc() }
        .doOnError { it.printStackTrace() }
        .sequential()
        .observeOn(scheduler)
        .blockingSubscribe { response ->
          workRequestProcessor.writeWorkResponse(response)
        }
      return@use 0
    }
  }

  private fun respondToRequest(request: WorkRequest): WorkResponse {
    ByteArrayOutputStream().use { baos ->
      // Create a print stream that the execution can write logs to
      val printStream = PrintStream(BufferedOutputStream(ByteArrayOutputStream()))
      var exitCode: Int
      try {
        // Sanity check the work request arguments
        val arguments =
          requireNotNull(request.arguments) {
            "Request with id ${request.requestId} " +
              "does not have arguments!"
          }
        require(arguments.isNotEmpty()) {
          "Request with id ${request.requestId} " +
            "does not have arguments!"
        }
        exitCode = workerWorkRequestCallback.processWorkRequest(arguments, printStream)
      } catch (e: Exception) {
        e.printStackTrace(printStream)
        exitCode = 1
      } finally {
        printStream.flush()
      }

      val output =
        arrayOf(baos.toString())
          .asSequence()
          .map { it.trim() }
          .filter { it.isNotEmpty() }
          .joinToString("\n")
      return WorkResponse(
        exitCode = exitCode,
        output = output,
        requestId = request.requestId,
      )
    }
  }
}
