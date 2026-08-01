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
class AndroidLintRunnerTest {
  @get:Rule
  val temporaryFolder = TemporaryFolder()

  @Before
  fun setUp() {
    Main.reset()
  }

  @Test
  fun `project includes explicit and aar embedded lint checks`() {
    val lintCli = writeStubLintJar("lint-cli.jar")
    val customRule = writeStubLintJar("custom-rule.jar")
    val aar = temporaryFolder.newFile("fixture.aar").toPath()
    val aarDirectory = temporaryFolder.newFolder("fixture-aar").toPath()
    val embeddedRule = aarDirectory.resolve("lint.jar")
    Files.copy(writeStubLintJar("embedded-rule.jar"), embeddedRule)
    val output = temporaryFolder.root.toPath().resolve("output.xml")
    val workingDirectory = temporaryFolder.newFolder("working").toPath()
    val args =
      AndroidLintActionArgs.parseArgs(
        listOf(
          "--android-lint-cli-tool",
          lintCli.toString(),
          "--label",
          "runner-test",
          "--output",
          output.toString(),
          "--custom-rule",
          customRule.toString(),
          "--classpath-aar",
          "$aar:$aarDirectory",
          "--compile-sdk-version",
          "34",
          "--java-language-level",
          "17",
          "--kotlin-language-level",
          "1.9",
        ),
      )

    val exitCode =
      AndroidLintRunner(AndroidLintCliInvokerCache(parentClassloader = javaClass.classLoader))
        .runAndroidLint(args, workingDirectory)

    val projectFile =
      Paths.get(
        Main.recordedRuns
          .single()
          .args
          .argumentAfter("--project"),
      )
    val projectXml = Files.readString(projectFile)
    assertThat(exitCode).isEqualTo(0)
    assertThat(projectXml)
      .contains("<lint-checks jar=\"${customRule.toAbsolutePath()}\"/>")
      .contains("<lint-checks jar=\"${embeddedRule.toAbsolutePath()}\"/>")
      .contains(
        "<aar extracted=\"${aarDirectory.toAbsolutePath()}\" file=\"${aar.toAbsolutePath()}\"/>",
      )
  }

  private fun writeStubLintJar(name: String): Path {
    val jar = temporaryFolder.newFile(name).toPath()
    JarOutputStream(Files.newOutputStream(jar)).use { stream ->
      stream.putNextEntry(JarEntry("stub.txt"))
      stream.write("stub".toByteArray())
      stream.closeEntry()
    }
    return jar
  }
}

private fun List<String>.argumentAfter(argument: String): String {
  val index = indexOf(argument)
  check(index >= 0 && index + 1 < size) { "Missing value for $argument in $this" }
  return this[index + 1]
}
