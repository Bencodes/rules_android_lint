import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import java.nio.file.Paths
import org.assertj.core.api.Assertions.assertThat


@RunWith(JUnit4::class)
class AndroidLintActionArgsTest {

  @Test
  fun `does parse all arguments`() {
    val parseArgs = AndroidLintActionArgs.parseArgs(
      args = arrayOf(
        "--module-name",
        "test_module_name",
        "--label",
        "test",
        "--src",
        "path/to/Foo.kt",
        "--output",
        "output.jar",
        "--project-config-output",
        "project.xml",
        "--resource",
        "path/to/resource/strings.xml",
        "--android-manifest",
        "AndroidManifest.xml",
        "--module-root",
        "/tmp/",
        "--baseline-file",
        "lib_lint_baseline.xml",
        "--config-file",
        "lint_config.xml",
        "--custom-rule",
        "custom_rule.jar",
        "--classpath",
        "classpath.jar",
        "--classpath",
        "classpath.aar",
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
        "--exitcode",
      ),
    )

    assertThat(parseArgs.moduleName).isEqualTo("test_module_name")
    assertThat(parseArgs.label).isEqualTo("test")
    assertThat(parseArgs.srcs).containsExactly(Paths.get("path/to/Foo.kt"))
    assertThat(parseArgs.output).isEqualTo(Paths.get("output.jar"))
    assertThat(parseArgs.projectConfigOutput).isEqualTo(Paths.get("project.xml"))
    assertThat(parseArgs.resources).containsExactly(Paths.get("path/to/resource/strings.xml"))
    assertThat(parseArgs.moduleRoot).isEqualTo(Paths.get("/tmp/"))
    assertThat(parseArgs.baselineFile).isEqualTo(Paths.get("lib_lint_baseline.xml"))
    assertThat(parseArgs.config).isEqualTo(Paths.get("lint_config.xml"))
    assertThat(parseArgs.customChecks).containsExactly(Paths.get("custom_rule.jar"))
    assertThat(parseArgs.classpath)
      .containsExactly(Paths.get("classpath.jar"), Paths.get("classpath.aar"))
    assertThat(parseArgs.autofix).isTrue
    assertThat(parseArgs.regenerateBaselineFile).isTrue
    assertThat(parseArgs.warningsAsErrors).isTrue
    assertThat(parseArgs.enableChecks).containsExactly("custom-check")
    assertThat(parseArgs.disableChecks).containsExactly("custom-disabled-check")
    assertThat(parseArgs.compileSdkVersion).isEqualTo("1.6")
    assertThat(parseArgs.javaLanguageLevel).isEqualTo("1.7")
    assertThat(parseArgs.kotlinLanguageLevel).isEqualTo("1.8")
    assertThat(parseArgs.exitCode).isTrue
  }
}
