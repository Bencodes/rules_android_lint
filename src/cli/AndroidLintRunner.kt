package com.rules.android.lint.cli

import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import kotlin.io.path.absolutePathString
import kotlin.io.path.exists
import kotlin.io.path.isRegularFile
import kotlin.io.path.pathString
import kotlin.io.path.writeText

internal class AndroidLintRunner(
  private val invokerCache: AndroidLintCliInvokerCache,
) {
  internal fun runAndroidLint(
    args: AndroidLintActionArgs,
    workingDirectory: Path,
  ): Int =
    when (args.mode) {
      "analyze" -> runAnalyze(args, workingDirectory)
      "report" -> runReport(args, workingDirectory)
      "legacy" -> runLegacy(args, workingDirectory)
      else -> error("Unknown --mode '${args.mode}'; expected one of: legacy, analyze, report")
    }

  /**
   * Analysis phase (`--analyze-only`). Analyzes this module in isolation and writes partial
   * results into [AndroidLintActionArgs.partialResults]. Produces no report and never fails on
   * lint issues — baseline, severity, and reporting are deferred to the report phase.
   */
  private fun runAnalyze(
    args: AndroidLintActionArgs,
    workingDirectory: Path,
  ): Int {
    val partialResults =
      requireNotNull(args.partialResults) { "--partial-results is required in analyze mode" }
    val rootDir = rootDir()
    val projectFile =
      writeProjectXml(
        args = args,
        workingDirectory = workingDirectory,
        rootDir = rootDir,
        partialResultsDir = partialResults,
        dependencyModules = emptyList(),
      )

    val lintArgs =
      buildList {
        add("--project")
        add(projectFile.pathString)
        add("--analyze-only")
        addCommonArgs(args, rootDir, workingDirectory)
      }

    return when (invoke(args, lintArgs, workingDirectory)) {
      AndroidLintCliInvoker.ERRNO_SUCCESS -> 0
      else -> AndroidLintCliInvoker.ERRNO_ERRORS
    }
  }

  /**
   * Report phase (`--report-only`). Merges this module's own partial results and those of its
   * first-party dependencies into the final XML report, applying baseline and severity.
   */
  private fun runReport(
    args: AndroidLintActionArgs,
    workingDirectory: Path,
  ): Int {
    val output = requireNotNull(args.output) { "--output is required in report mode" }
    val partialResults =
      requireNotNull(args.partialResults) { "--partial-results is required in report mode" }
    val baselineFile = stageBaseline(args, workingDirectory)
    val rootDir = rootDir()
    val projectFile =
      writeProjectXml(
        args = args,
        workingDirectory = workingDirectory,
        rootDir = rootDir,
        partialResultsDir = partialResults,
        dependencyModules =
          args.dependencyPartialResults.map { (name, dir) -> LintDependencyModule(name, dir) },
      )

    val lintArgs =
      buildList {
        add("--project")
        add(projectFile.pathString)
        add("--report-only")
        add("--xml")
        add(output.pathString)
        add("--exitcode")
        add("--baseline")
        add(baselineFile.pathString)
        add("--update-baseline")
        addReportFilters(args)
        addCommonArgs(args, rootDir, workingDirectory)
      }

    // When first-party dependency partial results are present, enable check-dependencies so the
    // merge phase reports their incidents (rather than analyzing — the partial results already
    // exist). Lint reads each dependency module's partial-results-dir instead of its sources.
    return reportExitCode(
      invoke(
        args,
        lintArgs,
        workingDirectory,
        enableCheckDependencies = args.dependencyPartialResults.isNotEmpty(),
      ),
    )
  }

  /** Legacy single-invocation behavior: analysis and reporting in one pass. */
  private fun runLegacy(
    args: AndroidLintActionArgs,
    workingDirectory: Path,
  ): Int {
    val output = requireNotNull(args.output) { "--output is required in legacy mode" }
    val baselineFile = stageBaseline(args, workingDirectory)
    val rootDir = rootDir()
    val projectFile =
      writeProjectXml(
        args = args,
        workingDirectory = workingDirectory,
        rootDir = rootDir,
        partialResultsDir = null,
        dependencyModules = emptyList(),
      )

    val lintArgs =
      buildList {
        add("--project")
        add(projectFile.pathString)
        add("--xml")
        add(output.pathString)
        add("--exitcode")
        add("--baseline")
        add(baselineFile.pathString)
        add("--update-baseline")
        addReportFilters(args)
        addCommonArgs(args, rootDir, workingDirectory)
      }

    return reportExitCode(
      invoke(
        args,
        lintArgs,
        workingDirectory,
        enableCheckDependencies = args.enableCheckDependencies,
      ),
    )
  }

  /** Arguments shared by all modes: project description, language levels, cache, SDK/JDK. */
  private fun MutableList<String>.addCommonArgs(
    args: AndroidLintActionArgs,
    rootDir: String,
    workingDirectory: Path,
  ) {
    add("--path-variables")
    add("PWD=$rootDir")
    add("--compile-sdk-version")
    add(args.compileSdkVersion)
    add("--java-language-level")
    add(args.javaLanguageLevel)
    add("--kotlin-language-level")
    add(args.kotlinLanguageLevel)
    add("--stacktrace")
    add("--quiet")
    add("--offline")
    // The lint analysis cache is ephemeral scratch in the Bazel sandbox: Bazel, not lint, owns
    // incrementality, and a persisted cache would be hidden, non-hermetic state. It is created
    // under the per-request working directory and discarded with it.
    val cacheDir = workingDirectory.resolve("android-cache")
    Files.createDirectories(cacheDir)
    add("--cache-dir")
    add(cacheDir.pathString)
    add("--client-id")
    add("cli")

    // Check selection must be consistent across analyze and report: the analyze phase decides
    // which detectors run, and the report phase finalizes them with the same set.
    if (args.config != null) {
      add("--config")
      add(args.config!!.pathString)
    }
    if (args.enableChecks.isNotEmpty()) {
      add("--enable")
      add(args.enableChecks.joinToString(","))
    }
    if (args.disableChecks.isNotEmpty()) {
      add("--disable")
      add(args.disableChecks.joinToString(","))
    }

    if (args.androidHome?.isNotEmpty() == true) {
      add("--sdk-home")
      add(Paths.get(rootDir, args.androidHome).absolutePathString())
    }
    if (args.jdkHome != null) {
      add("--jdk-home")
      add(args.jdkHome!!.absolutePathString())
    }
  }

  /** Reporting-only filters: warnings-as-errors handling, applied in report and legacy modes. */
  private fun MutableList<String>.addReportFilters(args: AndroidLintActionArgs) {
    if (args.warningsAsErrors) {
      add("-Werror")
    } else {
      add("--nowarn")
    }
  }

  private fun writeProjectXml(
    args: AndroidLintActionArgs,
    workingDirectory: Path,
    rootDir: String,
    partialResultsDir: Path?,
    dependencyModules: List<LintDependencyModule>,
  ): Path {
    val aarLintRuleJars = collectAarLintRuleJars(args.classpathAarPairs)
    val projectFile = workingDirectory.resolve("${args.label}_project_config.xml")
    Files.createFile(projectFile)

    // During partial analysis (analyze/report), AAR dependency projects must carry a non-null
    // partial-results-dir or lint's partial-analysis detectors NPE dereferencing it. They are not
    // analyzed, so an ephemeral shared scratch directory suffices.
    val aarPartialResultsScratchDir =
      if (partialResultsDir != null) {
        workingDirectory.resolve("aar-partial-results").also { Files.createDirectories(it) }
      } else {
        null
      }

    projectFile.writeText(
      createProjectXMLString(
        moduleName = args.label,
        rootDir = rootDir,
        srcs = args.srcs.sortedDescending(),
        resources = args.resources.sortedDescending(),
        androidManifest = args.androidManifest,
        classpathJars = args.classpath.sortedDescending(),
        classpathAars = emptyList(),
        classpathExtractedAarDirectories = args.classpathAarPairs,
        customLintChecks = (args.customChecks + aarLintRuleJars).sortedDescending(),
        partialResultsDir = partialResultsDir,
        dependencyModules = dependencyModules,
        aarPartialResultsScratchDir = aarPartialResultsScratchDir,
      ),
    )
    return projectFile
  }

  /**
   * Stages the input baseline into the working directory so lint can update it in place without
   * mutating the (read-only) source baseline. Returns the path of the staged baseline.
   */
  private fun stageBaseline(
    args: AndroidLintActionArgs,
    workingDirectory: Path,
  ): Path {
    val baselineFile = workingDirectory.resolve("${args.label}_lint_baseline")
    if (!args.regenerateBaselineFile && args.baselineFile != null) {
      Files.copy(args.baselineFile!!, baselineFile)
    }
    return baselineFile
  }

  private fun invoke(
    args: AndroidLintActionArgs,
    lintArgs: List<String>,
    workingDirectory: Path,
    enableCheckDependencies: Boolean = false,
  ): Int {
    val lintUserHomeFolder = workingDirectory.resolve("lint-user-home")
    Files.createDirectories(lintUserHomeFolder)

    val invoker = invokerCache.acquire(listOf(args.androidLintCliTool))
    return try {
      System.setProperty("user.home", lintUserHomeFolder.toString())
      invoker.invoke(
        args = lintArgs.toTypedArray(),
        enableCheckDependencies = enableCheckDependencies,
      )
    } finally {
      invokerCache.release(invoker)
    }
  }

  private fun reportExitCode(exitCode: Int): Int =
    when (exitCode) {
      AndroidLintCliInvoker.ERRNO_SUCCESS,
      AndroidLintCliInvoker.ERRNO_CREATED_BASELINE,
      -> 0

      else -> exitCode
    }

  private fun rootDir(): String = System.getenv("PWD") ?: Paths.get("").toAbsolutePath().pathString
}

/**
 * Collects the custom lint rule jars embedded in AARs. Each pair is (aar file, extracted
 * directory); an AAR's lint.jar lives at the root of the extracted directory.
 */
internal fun collectAarLintRuleJars(classpathAarPairs: List<Pair<Path, Path>>): List<Path> =
  classpathAarPairs
    .map { (_, extractedDirectory) -> extractedDirectory.resolve("lint.jar") }
    .filter { it.exists() && it.isRegularFile() }
