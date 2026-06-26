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
import java.nio.file.Paths
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

  @Test
  fun `lint runs with an invocation-scoped user home`() {
    val cache = AndroidLintCliInvokerCache()
    val executor = AndroidLintAction.AndroidLintExecutor(cache)
    val jar = writeStubLintJar()

    val exitCode = executor.processWorkRequest(workRequestArgs(jar, "user-home"), System.err)

    assertThat(exitCode).isEqualTo(0)
    assertThat(Main.recordedRuns).hasSize(1)
    assertThat(Main.recordedRuns.single().userHome)
      .contains("lint-user-home")
  }

  @Test
  fun `lint receives sdk home from android home`() {
    val cache = AndroidLintCliInvokerCache()
    val executor = AndroidLintAction.AndroidLintExecutor(cache)
    val jar = writeStubLintJar()

    val exitCode =
      executor.processWorkRequest(
        workRequestArgs(
          lintJar = jar,
          label = "android-home",
          extraArgs =
            listOf(
              "--android-home",
              "external/androidsdk",
            ),
        ),
        System.err,
      )

    val rootDir = Paths.get(System.getenv("PWD")).toAbsolutePath().normalize()
    val sdkHome =
      Main.recordedRuns
        .single()
        .args
        .argumentAfter("--sdk-home")

    assertThat(exitCode).isEqualTo(0)
    assertThat(sdkHome).isEqualTo(rootDir.resolve("external/androidsdk").toString())
  }

  private fun workRequestArgs(
    lintJar: Path,
    label: String,
    extraArgs: List<String> = emptyList(),
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
    ) + extraArgs
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

private fun List<String>.argumentAfter(argument: String): String {
  val argumentIndex = indexOf(argument)
  assertThat(argumentIndex)
    .describedAs("argument index for %s in %s", argument, this)
    .isGreaterThanOrEqualTo(0)
  assertThat(argumentIndex + 1)
    .describedAs("value index for %s in %s", argument, this)
    .isLessThan(size)
  return this[argumentIndex + 1]
}
