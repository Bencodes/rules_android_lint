package com.rules.android.lint.worker

import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.PrintStream
import java.nio.charset.StandardCharsets

@RunWith(JUnit4::class)
class WorkerIOTest {

  @Test
  @Throws(Exception::class)
  fun testWorkerIO_doesWrapSystemStreams() {
    // Save the original streams
    val originalInputStream = System.`in`
    val originalOutputStream = System.out
    val originalErrorStream = System.err

    // Swap in the test streams to assert against
    val byteArrayInputStream = ByteArrayInputStream(ByteArray(0))
    val outputBuffer = PrintStream(ByteArrayOutputStream(), true)
    val errorBuffer = PrintStream(ByteArrayOutputStream(), true)
    System.setIn(byteArrayInputStream)
    System.setOut(outputBuffer)
    System.setErr(errorBuffer)

    try {
      WorkerIO().use { io ->
        // Redirect the system streams
        io.redirectSystemStreams()

        // Assert that the WorkerIO returns the correct wrapped streams and the System instance
        // has been swapped out with the wrapped one. System.in should be untouched.
        assertThat(System.`in`).isSameAs(byteArrayInputStream)

        assertThat(io.output).isSameAs(outputBuffer)
        assertThat(System.out).isSameAs(errorBuffer)

        assertThat(io.err).isSameAs(errorBuffer)
        assertThat(System.err).isSameAs(errorBuffer)
      }
    } finally {
      // Swap back in the original streams
      System.setIn(originalInputStream)
      System.setOut(originalOutputStream)
      System.setErr(originalErrorStream)

      outputBuffer.close()
      errorBuffer.close()
      byteArrayInputStream.close()
    }
  }

  @Test
  @Throws(Exception::class)
  fun testWorkerIO_doesWriteSystemOutToSystemError() {
    // Save the original streams
    val originalInputStream = System.`in`
    val originalOutputStream = System.out
    val originalErrorStream = System.err

    // Swap in the test streams to assert against
    val byteArrayInputStream = ByteArrayInputStream(ByteArray(0))
    val byteArrayOutputStream = ByteArrayOutputStream()
    val outputBuffer = PrintStream(byteArrayOutputStream, true)
    System.setIn(byteArrayInputStream)
    System.setOut(outputBuffer)
    System.setErr(outputBuffer)
    try {
      WorkerIO().use { io ->
        // Redirect the system streams
        io.redirectSystemStreams()
        var captured = String(byteArrayOutputStream.toByteArray(), StandardCharsets.UTF_8)
        assertThat(captured).isEmpty()

        // Assert that the standard out/error stream redirect to our own streams
        println("This is a standard out message!")
        System.err.println("This is a standard error message!")
        captured = String(byteArrayOutputStream.toByteArray(), StandardCharsets.UTF_8)
        byteArrayOutputStream.reset()
        assertThat(captured)
          .isEqualTo("This is a standard out message!\nThis is a standard error message!\n")
      }
    } finally {
      // Swap back in the original streams
      System.setIn(originalInputStream)
      System.setOut(originalOutputStream)
      System.setErr(originalErrorStream)
      outputBuffer.close()
      byteArrayInputStream.close()
    }
  }
}
