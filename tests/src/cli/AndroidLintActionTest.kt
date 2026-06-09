package com.rules.android.lint.cli

import com.android.tools.lint.Main
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import java.nio.file.Files
import java.nio.file.Path
import java.util.jar.JarEntry
import java.util.jar.JarOutputStream

@RunWith(JUnit4::class)
class AndroidLintActionTest {
  @get:Rule
  val temporaryFolder = TemporaryFolder()

  @Before
  fun setUp() {
    Main.reset()
  }

  @Test
  fun `sequential work requests reuse a single cached lint classloader`() {
    val cache = AndroidLintCliInvokerCache()
    val executor = AndroidLintAction.AndroidLintExecutor(cache)
    val jar = writeStubLintJar()

    val firstExitCode = executor.processWorkRequest(workRequestArgs(jar, "first"), System.err)
    val secondExitCode = executor.processWorkRequest(workRequestArgs(jar, "second"), System.err)

    assertThat(firstExitCode).isEqualTo(0)
    assertThat(secondExitCode).isEqualTo(0)
    assertThat(Main.recordedRuns).hasSize(2)
    assertThat(cache.createdCount).isEqualTo(1)
  }

  @Test
  fun `lint issue exit codes do not fail the work request or evict the cache`() {
    // By design the action always succeeds and produces a report; enforcement happens in the
    // android_lint_test validator. A lint exit code must not kill or degrade the worker.
    val cache = AndroidLintCliInvokerCache()
    val executor = AndroidLintAction.AndroidLintExecutor(cache)
    val jar = writeStubLintJar()

    Main.exitCode = AndroidLintCliInvoker.ERRNO_ERRORS
    val errorsExitCode = executor.processWorkRequest(workRequestArgs(jar, "failing"), System.err)

    Main.exitCode = AndroidLintCliInvoker.ERRNO_SUCCESS
    val successExitCode = executor.processWorkRequest(workRequestArgs(jar, "passing"), System.err)

    assertThat(errorsExitCode).isEqualTo(0)
    assertThat(successExitCode).isEqualTo(0)
    assertThat(Main.recordedRuns).hasSize(2)
    assertThat(cache.createdCount).isEqualTo(1)
  }

  private fun workRequestArgs(
    lintJar: Path,
    label: String,
  ): List<String> {
    val output = temporaryFolder.root.toPath().resolve("$label-output.xml")
    return listOf(
      "--android-lint-cli-tool",
      lintJar.toString(),
      "--label",
      label,
      "--output",
      output.toString(),
      "--compile-sdk-version",
      "34",
      "--java-language-level",
      "17",
      "--kotlin-language-level",
      "1.9",
    )
  }

  private fun writeStubLintJar(): Path {
    val jar = temporaryFolder.newFile("lint.jar").toPath()
    JarOutputStream(Files.newOutputStream(jar)).use { stream ->
      stream.putNextEntry(JarEntry("stub.txt"))
      stream.write("stub".toByteArray())
      stream.closeEntry()
    }
    return jar
  }
}
