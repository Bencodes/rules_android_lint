package com.rules.android.lint.cli

import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import kotlin.io.path.absolutePathString
import kotlin.io.path.exists
import kotlin.io.path.isRegularFile
import kotlin.io.path.pathString
import kotlin.io.path.writeText

internal class AndroidLintRunner {
  internal fun runAndroidLint(
    args: AndroidLintActionArgs,
    workingDirectory: Path,
  ): Int {
    // Create the input baseline file. This is either a copy of the existing baseline
    // or a new temp one that can be written to
    val baselineFile = workingDirectory.resolve("${args.label}_lint_baseline")
    if (!args.regenerateBaselineFile && args.baselineFile != null) {
      Files.copy(args.baselineFile!!, baselineFile)
    }

    // Collect the custom lint rules from the unpacked aars
    val aarLintRuleJars =
      args.classpathAarPairs
        .asSequence()
        .map { it.first.resolve("lint.jar") }
        .filter { it.exists() && it.isRegularFile() }

    // Create the project configuration file for lint
    val projectFile = workingDirectory.resolve("${args.label}_project_config.xml")
    Files.createFile(projectFile)

    val rootDir = System.getenv("PWD")
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
      ),
    )

    // Run Android Lint
    val androidCacheFolder = workingDirectory.resolve("android-cache")
    Files.createDirectory(androidCacheFolder)
    val invoker = AndroidLintCliInvoker.createUsingJars(jars = arrayOf(args.androidLintCliTool))
    val exitCode =
      invokeAndroidLintCLI(
        invoker = invoker,
        actionArgs = args,
        rootDirPath = rootDir,
        projectFilePath = projectFile,
        baselineFilePath = baselineFile,
        cacheDirectoryPath = androidCacheFolder,
      )

    return when (exitCode) {
      AndroidLintCliInvoker.ERRNO_SUCCESS,
      AndroidLintCliInvoker.ERRNO_CREATED_BASELINE,
      -> 0

      else -> exitCode
    }
  }

  private fun invokeAndroidLintCLI(
    invoker: AndroidLintCliInvoker,
    actionArgs: AndroidLintActionArgs,
    rootDirPath: String,
    projectFilePath: Path,
    baselineFilePath: Path,
    cacheDirectoryPath: Path,
  ): Int {
    val args =
      mutableListOf(
        "--project",
        projectFilePath.pathString,
        "--xml",
        actionArgs.output.pathString,
        "--path-variables",
        "PWD=$rootDirPath",
        "--exitcode",
        "--compile-sdk-version",
        actionArgs.compileSdkVersion,
        "--java-language-level",
        actionArgs.javaLanguageLevel,
        "--kotlin-language-level",
        actionArgs.kotlinLanguageLevel,
        "--stacktrace",
        "--quiet",
        "--offline",
        "--baseline",
        baselineFilePath.pathString,
        "--update-baseline",
        "--cache-dir",
        cacheDirectoryPath.pathString,
        "--client-id",
        "cli",
      )
    if (actionArgs.warningsAsErrors) {
      args.add("-Werror")
    } else {
      args.add("--nowarn")
    }
    if (actionArgs.config != null) {
      args.add("--config")
      args.add(actionArgs.config!!.pathString)
    }
    if (actionArgs.enableChecks.isNotEmpty()) {
      args.add("--enable")
      args.add(actionArgs.enableChecks.joinToString(","))
    }
    if (actionArgs.disableChecks.isNotEmpty()) {
      args.add("--disable")
      args.add(actionArgs.disableChecks.joinToString(","))
    }

    if (actionArgs.androidHome?.isNotEmpty() != null) {
      var androidHomePath =
        Paths.get(System.getenv("PWD"), actionArgs.androidHome).absolutePathString()
      args.add("--sdk-home")
      args.add(androidHomePath)
    }

    if (actionArgs.jdkHome != null) {
      val jdkHome = actionArgs.jdkHome!!
      args.add("--jdk-home")
      args.add(jdkHome.absolutePathString())
    }

    invoker.setCheckDependencies(actionArgs.enableCheckDependencies)
    return invoker.invoke(args.toTypedArray())
  }
}
