package com.rules.android.lint.cli

import com.rules.android.lint.worker.Worker
import io.bazel.worker.PersistentWorker
import io.bazel.worker.Status
import io.bazel.worker.Work
import io.bazel.worker.WorkerContext
import java.io.PrintStream
import java.nio.file.Files
import javax.inject.Inject
import kotlin.system.exitProcess

object AndroidLintPersistentWorker {
  @JvmStatic
  fun main(args: Array<String>) {
    if ("--persistent_worker" in args) {
      val worker = PersistentWorker()
      worker.start(AndroidLint()).run(::exitProcess)
      return
    }
    val worker = Worker.fromArgs(args, AndroidLintExecutor())
    val exitCode = worker.processRequests()
    exitProcess(exitCode)
  }

  private class AndroidLint
    @Inject
    constructor() : Work {
      override fun invoke(
        ctx: WorkerContext.TaskContext,
        args: Iterable<String>,
      ): Status {
        val workingDirectory = Files.createTempDirectory("rules")
        try {
          val runner = AndroidLintRunner()
          val parsedArgs = AndroidLintActionArgs.parseArgs(args.toList())
          val result = runner.runAndroidLint(parsedArgs, workingDirectory)
          if (result == 0) {
            return Status.SUCCESS
          }
          return Status.ERROR
        } catch (exception: Exception) {
          return Status.ERROR
        } finally {
          try {
            workingDirectory.toFile().deleteRecursively()
          } catch (e: Exception) {
            e.printStackTrace()
          }
        }
      }
    }

  private class AndroidLintExecutor : Worker.WorkRequestCallback {
    override fun processWorkRequest(
      args: List<String>,
      printStream: PrintStream,
    ): Int {
      val workingDirectory = Files.createTempDirectory("rules")

      try {
        val runner = AndroidLintRunner()
        val parsedArgs = AndroidLintActionArgs.parseArgs(args)
        return runner.runAndroidLint(parsedArgs, workingDirectory)
      } catch (exception: Exception) {
        exception.printStackTrace()
        return 1
      } finally {
        try {
          workingDirectory.toFile().deleteRecursively()
        } catch (e: Exception) {
          e.printStackTrace()
        }
      }
    }
  }
}
