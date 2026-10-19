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
        runnerArgs(lintCli, "runner-test") +
          listOf(
            "--output",
            output.toString(),
            "--custom-rule",
            customRule.toString(),
            "--classpath-aar",
            "$aar:$aarDirectory",
          ),
      )

    val exitCode =
      AndroidLintRunner(AndroidLintCliInvokerCache(parentClassloader = javaClass.classLoader))
        .runAndroidLint(args, workingDirectory)

    val projectXml = projectXml(Main.recordedRuns.single())
    assertThat(exitCode).isEqualTo(0)
    assertThat(projectXml)
      .contains("<lint-checks jar=\"${customRule.toAbsolutePath()}\"/>")
      .contains("<lint-checks jar=\"${embeddedRule.toAbsolutePath()}\"/>")
      .contains(
        "<aar extracted=\"${aarDirectory.toAbsolutePath()}\" file=\"${aar.toAbsolutePath()}\"/>",
      )
  }

  @Test
  fun `analyze mode writes an isolated partial-results project`() {
    val lintCli = writeStubLintJar("lint-cli.jar")
    val source = temporaryFolder.newFile("Analyze.kt").toPath()
    val resource = temporaryFolder.newFile("analyze.xml").toPath()
    val manifest = temporaryFolder.newFile("AndroidManifest.xml").toPath()
    val partialResults = temporaryFolder.newFolder("analyze-partial-results").toPath()
    val workingDirectory = temporaryFolder.newFolder("analyze-working").toPath()
    val args =
      AndroidLintActionArgs.parseArgs(
        runnerArgs(lintCli, "analyze-test") +
          listOf(
            "--mode",
            "analyze",
            "--android",
            "--library",
            "--test-sources",
            "--partial-results",
            partialResults.toString(),
            "--src",
            source.toString(),
            "--resource",
            resource.toString(),
            "--android-manifest",
            manifest.toString(),
          ),
      )

    val exitCode =
      AndroidLintRunner(AndroidLintCliInvokerCache(parentClassloader = javaClass.classLoader))
        .runAndroidLint(args, workingDirectory)

    val run = Main.recordedRuns.single()
    val projectXml = projectXml(run)
    assertThat(exitCode).isEqualTo(0)
    assertThat(run.checkDependencies).isFalse()
    assertThat(run.args)
      .contains("--analyze-only")
      .doesNotContain("--report-only", "--xml", "--exitcode", "--baseline", "--update-baseline")
    assertThat(projectXml)
      .contains(
        "<module android=\"true\" library=\"true\" name=\"analyze-test\" " +
          "partial-results-dir=\"${partialResults.toAbsolutePath()}\">",
      ).contains("<src file=\"$source\" test=\"true\"/>")
      .contains("<resource file=\"$resource\"/>")
      .contains("<manifest file=\"$manifest\"/>")
      .doesNotContain("<dep module=")
    assertThat(Files.isDirectory(workingDirectory.resolve("aar-partial-results"))).isTrue()
  }

  @Test
  fun `report mode merges dependency partial results with their module identities`() {
    val lintCli = writeStubLintJar("lint-cli.jar")
    val ownPartialResults = temporaryFolder.newFolder("report-partial-results").toPath()
    val androidDependencyResults = temporaryFolder.newFolder("android-dependency-results").toPath()
    val javaDependencyResults = temporaryFolder.newFolder("java-dependency-results").toPath()
    val androidDependencySource = temporaryFolder.newFile("AndroidDependency.kt").toPath()
    val androidDependencyResource = temporaryFolder.newFile("dependency_strings.xml").toPath()
    val androidDependencyManifest = temporaryFolder.newFile("DependencyManifest.xml").toPath()
    val androidDependencyClasspath = writeStubLintJar("android-dependency.jar")
    val androidDependencyAar = temporaryFolder.newFile("android-dependency.aar").toPath()
    val androidDependencyAarDirectory =
      temporaryFolder.newFolder("android-dependency-aar").toPath()
    val dependencyLintRule = androidDependencyAarDirectory.resolve("lint.jar")
    Files.copy(writeStubLintJar("dependency-lint-rule.jar"), dependencyLintRule)
    val javaDependencySource = temporaryFolder.newFile("JavaDependency.java").toPath()
    val androidDependencyModel =
      writeDependencyModel(
        name = "android-dependency",
        partialResults = androidDependencyResults,
        isAndroid = true,
        isLibrary = true,
        srcs = listOf(androidDependencySource),
        resources = listOf(androidDependencyResource),
        androidManifest = androidDependencyManifest,
        classpathJars = listOf(androidDependencyClasspath),
        classpathAars = listOf(androidDependencyAar to androidDependencyAarDirectory),
      )
    val javaDependencyModel =
      writeDependencyModel(
        name = "java-dependency",
        partialResults = javaDependencyResults,
        isAndroid = false,
        isLibrary = true,
        srcs = listOf(javaDependencySource),
      )
    val output = temporaryFolder.root.toPath().resolve("report.xml")
    val workingDirectory = temporaryFolder.newFolder("report-working").toPath()
    val args =
      AndroidLintActionArgs.parseArgs(
        runnerArgs(lintCli, "report-test") +
          listOf(
            "--mode",
            "report",
            "--android",
            "--library",
            "--partial-results",
            ownPartialResults.toString(),
            "--dependency-model",
            androidDependencyModel.toString(),
            "--dependency-model",
            javaDependencyModel.toString(),
            "--output",
            output.toString(),
            "--warnings-as-errors",
          ),
      )

    val exitCode =
      AndroidLintRunner(AndroidLintCliInvokerCache(parentClassloader = javaClass.classLoader))
        .runAndroidLint(args, workingDirectory)

    val run = Main.recordedRuns.single()
    val projectXml = projectXml(run)
    assertThat(exitCode).isEqualTo(0)
    assertThat(run.checkDependencies).isTrue()
    assertThat(run.args)
      .contains("--report-only", "--exitcode", "--update-baseline", "-Werror")
      .doesNotContain("--analyze-only", "--nowarn")
    assertThat(run.args.argumentAfter("--xml")).isEqualTo(output.toString())
    assertThat(run.args.argumentAfter("--baseline"))
      .isEqualTo(workingDirectory.resolve("report-test_lint_baseline").toString())
    assertThat(projectXml)
      .contains(
        "<module android=\"true\" library=\"true\" name=\"report-test\" " +
          "partial-results-dir=\"${ownPartialResults.toAbsolutePath()}\">",
      ).contains("<dep module=\"android-dependency\"/>")
      .contains("<dep module=\"java-dependency\"/>")
      .contains(
        "<module android=\"true\" library=\"true\" name=\"android-dependency\" " +
          "partial-results-dir=\"${androidDependencyResults.toAbsolutePath()}\">",
      ).contains("<src file=\"$androidDependencySource\"/>")
      .contains("<resource file=\"$androidDependencyResource\"/>")
      .contains("<manifest file=\"$androidDependencyManifest\"/>")
      .contains("<classpath jar=\"${androidDependencyClasspath.toAbsolutePath()}\"/>")
      .contains(
        "<aar extracted=\"${androidDependencyAarDirectory.toAbsolutePath()}\" " +
          "file=\"${androidDependencyAar.toAbsolutePath()}\"",
      ).contains("<lint-checks jar=\"${dependencyLintRule.toAbsolutePath()}\"/>")
      .contains(
        "<module android=\"false\" library=\"true\" name=\"java-dependency\" " +
          "partial-results-dir=\"${javaDependencyResults.toAbsolutePath()}\">",
      ).contains(
        "<src file=\"$javaDependencySource\"/>",
      )
  }

  private fun runnerArgs(
    lintCli: Path,
    label: String,
  ): List<String> =
    listOf(
      "--android-lint-cli-tool",
      lintCli.toString(),
      "--label",
      label,
      "--compile-sdk-version",
      "34",
      "--java-language-level",
      "17",
      "--kotlin-language-level",
      "1.9",
    )

  private fun projectXml(run: Main.RunRecord): String =
    Files.readString(Paths.get(run.args.argumentAfter("--project")))

  private fun writeStubLintJar(name: String): Path {
    val jar = temporaryFolder.newFile(name).toPath()
    JarOutputStream(Files.newOutputStream(jar)).use { stream ->
      stream.putNextEntry(JarEntry("stub.txt"))
      stream.write("stub".toByteArray())
      stream.closeEntry()
    }
    return jar
  }

  private fun writeDependencyModel(
    name: String,
    partialResults: Path,
    isAndroid: Boolean,
    isLibrary: Boolean,
    srcs: List<Path> = emptyList(),
    resources: List<Path> = emptyList(),
    androidManifest: Path? = null,
    classpathJars: List<Path> = emptyList(),
    classpathAars: List<Pair<Path, Path>> = emptyList(),
  ): Path {
    val model = temporaryFolder.newFile("$name-model.json").toPath()
    val serializedAars =
      classpathAars.joinToString(prefix = "[", postfix = "]") { (aar, extracted) ->
        """{"aar": ${aar.toString().jsonString()}, "extracted": ${
          extracted.toString().jsonString()
        }}"""
      }
    val json =
      """
      {
        "name": ${name.jsonString()},
        "partialResultsDir": ${partialResults.toString().jsonString()},
        "isAndroid": $isAndroid,
        "isLibrary": $isLibrary,
        "srcs": ${srcs.toJson()},
        "resources": ${resources.toJson()},
        "androidManifest": ${androidManifest?.toString()?.jsonString() ?: "null"},
        "classpathJars": ${classpathJars.toJson()},
        "classpathAars": [],
        "classpathExtractedAarDirectories": $serializedAars
      }
      """.trimIndent()
    Files.writeString(model, json)
    return model
  }
}

private fun String.jsonString(): String = "\"${replace("\\", "\\\\").replace("\"", "\\\"")}\""

private fun List<Path>.toJson(): String =
  joinToString(prefix = "[", postfix = "]") { it.toString().jsonString() }

private fun List<String>.argumentAfter(argument: String): String {
  val index = indexOf(argument)
  check(index >= 0 && index + 1 < size) { "Missing value for $argument in $this" }
  return this[index + 1]
}
