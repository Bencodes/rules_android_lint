package com.rules.android.lint.cli

import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import java.nio.file.Paths

@RunWith(JUnit4::class)
class AndroidLintActionArgsTest {
  @Test
  fun `does parse all arguments`() {
    val parseArgs =
      AndroidLintActionArgs.parseArgs(
        args =
          listOf(
            "--label",
            "test",
            "--android-lint-cli-tool",
            "path/to/cli.jar",
            "--android-home",
            "external/androidsdk",
            "--src",
            "path/to/Foo.kt",
            "--output",
            "output.jar",
            "--resource",
            "path/to/resource/strings.xml",
            "--android-manifest",
            "AndroidManifest.xml",
            "--baseline-file",
            "lib_lint_baseline.xml",
            "--config-file",
            "lint_config.xml",
            "--custom-rule",
            "custom_rule.jar",
            "--classpath-jar",
            "classpath.jar",
            "--classpath-aar",
            "classpath.aar:aar_directory",
            "--autofix",
            "--regenerate-baseline-files",
            "--warnings-as-errors",
            "--enable-check",
            "custom-check",
            "--disable-check",
            "custom-disabled-check",
            "--compile-sdk-version",
            "1.6",
            "--java-language-level",
            "1.7",
            "--kotlin-language-level",
            "1.8",
            "--enable-check-dependencies",
          ),
      )

    assertThat(parseArgs.label).isEqualTo("test")
    assertThat(parseArgs.androidHome).isEqualTo("external/androidsdk")
    assertThat(parseArgs.srcs).containsExactly(Paths.get("path/to/Foo.kt"))
    assertThat(parseArgs.output).isEqualTo(Paths.get("output.jar"))
    assertThat(parseArgs.resources).containsExactly(Paths.get("path/to/resource/strings.xml"))
    assertThat(parseArgs.baselineFile).isEqualTo(Paths.get("lib_lint_baseline.xml"))
    assertThat(parseArgs.config).isEqualTo(Paths.get("lint_config.xml"))
    assertThat(parseArgs.customChecks).containsExactly(Paths.get("custom_rule.jar"))
    assertThat(parseArgs.classpath)
      .containsExactly(Paths.get("classpath.jar"))
    assertThat(parseArgs.classpathAarPairs)
      .containsExactly(Paths.get("classpath.aar") to Paths.get("aar_directory"))
    assertThat(parseArgs.autofix).isTrue
    assertThat(parseArgs.regenerateBaselineFile).isTrue
    assertThat(parseArgs.warningsAsErrors).isTrue
    assertThat(parseArgs.enableChecks).containsExactly("custom-check")
    assertThat(parseArgs.disableChecks).containsExactly("custom-disabled-check")
    assertThat(parseArgs.compileSdkVersion).isEqualTo("1.6")
    assertThat(parseArgs.javaLanguageLevel).isEqualTo("1.7")
    assertThat(parseArgs.kotlinLanguageLevel).isEqualTo("1.8")
    assertThat(parseArgs.enableCheckDependencies).isTrue()
    // Mode defaults to legacy when not specified.
    assertThat(parseArgs.mode).isEqualTo("legacy")
  }

  @Test
  fun `does parse analyze and report mode arguments`() {
    val parseArgs =
      AndroidLintActionArgs.parseArgs(
        args =
          listOf(
            "--label",
            "test",
            "--android-lint-cli-tool",
            "path/to/cli.jar",
            "--mode",
            "report",
            "--android",
            "--library",
            "--partial-results",
            "path/to/partial",
            "--dependency-partial-results",
            "dep_a=path/to/dep_a/partial",
            "--android-dependency",
            "dep_a",
            "--library-dependency",
            "dep_a",
            "--dependency-partial-results",
            "dep_b=path/to/dep_b/partial",
            "--compile-sdk-version",
            "34",
            "--java-language-level",
            "17",
            "--kotlin-language-level",
            "1.9",
          ),
      )

    assertThat(parseArgs.mode).isEqualTo("report")
    assertThat(parseArgs.isAndroid).isTrue()
    assertThat(parseArgs.isLibrary).isTrue()
    assertThat(parseArgs.partialResults).isEqualTo(Paths.get("path/to/partial"))
    assertThat(parseArgs.dependencyPartialResults).containsExactly(
      "dep_a" to Paths.get("path/to/dep_a/partial"),
      "dep_b" to Paths.get("path/to/dep_b/partial"),
    )
    assertThat(parseArgs.androidDependencies).containsExactly("dep_a")
    assertThat(parseArgs.libraryDependencies).containsExactly("dep_a")
    // Output is optional now; absent here.
    assertThat(parseArgs.output).isNull()
  }
}
