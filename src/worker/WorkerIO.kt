package com.rules.android.lint.worker

import java.io.InputStream
import java.io.PrintStream

internal class WorkerIO(
  val input: InputStream = System.`in`,
  val output: PrintStream = System.out,
  val err: PrintStream = System.err,
) : AutoCloseable {

  fun redirectSystemStreams(): WorkerIO {
    System.setOut(err)
    return this
  }

  override fun close() {
    System.setOut(output)
  }
}
