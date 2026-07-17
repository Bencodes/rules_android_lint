package com.rules.android.lint.cli

import com.xenomachina.argparser.ArgParser
import com.xenomachina.argparser.default
import java.io.File
import java.nio.charset.Charset
import java.nio.file.Path
import java.nio.file.Paths

internal class AndroidLintActionArgs(
  parser: ArgParser,
) {
  private val argsParserPathTransformer: String.() -> Path = {
    Paths.get(this)
  }
  private val argsParserAarPairPathTransformer: String.() -> Pair<Path, Path> = {
    val (aar, aarDir) = this.split(":")
    Pair(Paths.get(aar), Paths.get(aarDir))
  }

  private val argsParserDependencyModuleTransformer: String.() -> Pair<String, Path> = {
    // Format: <module-name>=<partial-results-dir>. The name is opaque and may itself contain
    // no '=', so split on the first occurrence only.
    val separator = this.indexOf('=')
    require(separator > 0) {
      "Error: --dependency-partial-results expects <module-name>=<dir>, got: $this"
    }
    Pair(this.substring(0, separator), Paths.get(this.substring(separator + 1)))
  }

  val androidLintCliTool: Path by parser.storing(
    names = arrayOf("--android-lint-cli-tool"),
    help = "",
    transform = argsParserPathTransformer,
  )

  val label: String by parser.storing(
    names = arrayOf("--label"),
    help = "",
  )

  // Execution mode. "legacy" runs analysis and reporting in a single invocation (the original
  // behavior). "analyze" runs `--analyze-only` and writes partial results. "report" runs
  // `--report-only`, merging the module's own and its dependencies' partial results into a report.
  val mode: String by parser
    .storing(
      names = arrayOf("--mode"),
      help = "One of: legacy, analyze, report",
    ).default { "legacy" }

  val isAndroid: Boolean by parser
    .flagging(
      names = arrayOf("--android"),
      help = "Model this project as an Android module.",
    ).default { false }

  val isLibrary: Boolean by parser
    .flagging(
      names = arrayOf("--library"),
      help = "Model this project as a library module.",
    ).default { false }

  // In analyze mode, the directory lint writes partial results into. In report mode, the directory
  // lint reads the module's own partial results from.
  val partialResults: Path? by parser
    .storing(
      names = arrayOf("--partial-results"),
      help = "",
      transform = argsParserPathTransformer,
    ).default { null }

  // First-party dependency partial results consumed in report mode, as <module-name>=<dir> pairs.
  val dependencyPartialResults: List<Pair<String, Path>> by parser
    .adding(
      names = arrayOf("--dependency-partial-results"),
      help = "",
      transform = argsParserDependencyModuleTransformer,
    ).default { emptyList() }

  // Names of dependency modules whose partial results came from Android projects.
  val androidDependencies: List<String> by parser
    .adding(
      names = arrayOf("--android-dependency"),
      help = "",
    ).default { emptyList() }

  // Names of dependency modules whose partial results came from library projects.
  val libraryDependencies: List<String> by parser
    .adding(
      names = arrayOf("--library-dependency"),
      help = "",
    ).default { emptyList() }

  val androidHome: String? by parser
    .storing(
      names = arrayOf("--android-home"),
      help = "The relative location of Android home",
    ).default { null }

  val jdkHome: Path? by parser
    .storing(
      names = arrayOf("--jdk-home"),
      help = "The relative location of JDK home",
      transform = argsParserPathTransformer,
    ).default { null }

  val srcs: List<Path> by parser.adding(
    names = arrayOf("--src"),
    help = "",
    transform = argsParserPathTransformer,
  )

  // The XML report output. Required in legacy and report modes; absent in analyze mode.
  val output: Path? by parser
    .storing(
      names = arrayOf("--output"),
      help = "",
      transform = argsParserPathTransformer,
    ).default { null }

  val resources: List<Path> by parser
    .adding(
      names = arrayOf("--resource"),
      help = "",
      transform = argsParserPathTransformer,
    ).default { emptyList() }

  val androidManifest: Path? by parser
    .storing(
      names = arrayOf("--android-manifest"),
      help = "",
      transform = argsParserPathTransformer,
    ).default { null }

  val baselineFile: Path? by parser
    .storing(
      names = arrayOf("--baseline-file"),
      help = "",
      transform = argsParserPathTransformer,
    ).default { null }

  val config: Path? by parser
    .storing(
      names = arrayOf("--config-file"),
      help = "",
      transform = argsParserPathTransformer,
    ).default { null }

  val customChecks: List<Path> by parser
    .adding(
      names = arrayOf("--custom-rule"),
      help = "",
      transform = argsParserPathTransformer,
    ).default { emptyList() }

  val classpath: List<Path> by parser
    .adding(
      names = arrayOf("--classpath-jar"),
      help = "",
      transform = argsParserPathTransformer,
    ).default { emptyList() }

  val classpathAarPairs: List<Pair<Path, Path>> by parser
    .adding(
      names = arrayOf("--classpath-aar"),
      help = "",
      transform = argsParserAarPairPathTransformer,
    ).default { emptyList() }

  val autofix: Boolean by parser
    .flagging(
      names = arrayOf("--autofix"),
      help = "TODO Not supported yet",
    ).default { false }

  val regenerateBaselineFile: Boolean by parser
    .flagging(
      names = arrayOf("--regenerate-baseline-files"),
      help = "",
    ).default { false }

  val warningsAsErrors: Boolean by parser
    .flagging(
      names = arrayOf("--warnings-as-errors"),
      help = "",
    ).default { false }

  val enableChecks: List<String> by parser
    .adding(
      names = arrayOf("--enable-check"),
      help = "",
    ).default { emptyList() }

  val disableChecks: List<String> by parser
    .adding(
      names = arrayOf("--disable-check"),
      help = "",
    ).default { emptyList() }

  val compileSdkVersion: String by parser.storing(
    names = arrayOf("--compile-sdk-version"),
    help = "",
  )

  val javaLanguageLevel: String by parser.storing(
    names = arrayOf("--java-language-level"),
    help = "",
  )

  val kotlinLanguageLevel: String by parser.storing(
    names = arrayOf("--kotlin-language-level"),
    help = "",
  )

  val enableCheckDependencies: Boolean by parser
    .flagging(
      names = arrayOf("--enable-check-dependencies"),
      help = "",
    ).default { false }

  companion object {
    internal fun parseArgs(args: List<String>): AndroidLintActionArgs {
      // TODO Need to handle the --flagfile argument here
      val unwrappedArgs: Array<String> =
        if (args.size == 1 && args[0].startsWith("@")) {
          File(args[0].removePrefix("@")).readLines(Charset.defaultCharset()).toTypedArray()
        } else {
          args.toTypedArray()
        }

      return AndroidLintActionArgs(ArgParser(unwrappedArgs))
    }
  }
}
