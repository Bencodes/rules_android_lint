import com.android.tools.lint.LintCliFlags
import com.android.tools.lint.Main
import java.nio.file.Files
import java.nio.file.Path
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import kotlin.io.path.*


internal class AndroidLintRunner {

  internal fun runAndroidLint(args: AndroidLintActionArgs, workingDirectory: Path): Int {
    // Create the input baseline file. This is either a copy of the existing baseline
    // or a new temp one that can be written to
    val baselineFile = workingDirectory.resolve("${args.moduleName}_lint_baseline")
    if (!args.regenerateBaselineFile && args.baselineFile != null) {
      Files.copy(args.baselineFile!!, baselineFile)
    }

    // Split the aars and jars
    val aars = args.classpath.filter { it.extension == "aar" }
    val jars = args.classpath.filter { it.extension == "jar" }
    require(aars.size + jars.size == args.classpath.size) { "Error: Classpath size mismatch" }

    // Unarchive the AARs to avoid lint having to do this work. This also prevents some
    // concurrency issues inside of Lint when multiplex workers are enabled
    val unpackedAars = unpackAars(aars, workingDirectory.resolve("aars"))

    // Collect the custom lint rules from the unpacked aars
    val aarLintRuleJars = unpackedAars
      .asSequence()
      .map { it.first.resolve("lint.jar") }
      .filter { it.exists() && it.isRegularFile() }

    // Create the project configuration file for lint
    val projectFile = args.projectConfigOutput
    Files.createFile(projectFile)
    projectFile.writeText(
      createProjectXMLString(
        moduleName = args.label,
        srcs = args.srcs.sortedDescending(),
        resources = args.resources.sortedDescending(),
        androidManifest = args.androidManifest,
        classpathJars = jars.sortedDescending(),
        classpathAars = emptyList(),
        classpathExtractedAarDirectories = unpackedAars,
        customLintChecks = (args.customChecks + aarLintRuleJars).sortedDescending(),
      ),
    )


    // Run Android Lint
    val exitCode = runAndroidLintCLI(args, projectFile, baselineFile)

    // Pure hacks to strip the relative paths out of the file locations. Lint doesn't offer any way to disable this
    // and if we parse-and-transform the baseline, we can't easily match format that Lint produces.
    args.output.readText().run {
      args.output.writeText(
        this.replace("../", "")
          .replace("file=\"" + args.moduleRoot.pathString + "/", "file=\""),
      )
    }

    /*
     * Exit Status:
     * 0   ->   Success.
     * 1   ->   Lint errors detected.
     * 2   ->   Lint usage.
     * 3   ->   Cannot clobber existing file.
     * 4   ->   Lint help.
     * 5   ->   Invalid command-line argument.
     * 6   ->   Created baseline file.
     * 100 ->   Internal continue.
     */
    return when (exitCode) {
      LintCliFlags.ERRNO_SUCCESS,
      LintCliFlags.ERRNO_CREATED_BASELINE,
      -> 0

      else -> exitCode
    }
  }

  private fun runAndroidLintCLI(
    actionArgs: AndroidLintActionArgs,
    projectFilePath: Path,
    baselineFilePath: Path,
  ): Int {
    val args = mutableListOf(
      "--project",
      projectFilePath.pathString,
      "--xml",
      actionArgs.output.pathString,
      "--exitcode",
      "--compile-sdk-version",
      actionArgs.compileSdkVersion,
      "--java-language-level",
      actionArgs.javaLanguageLevel,
      "--kotlin-language-level",
      actionArgs.kotlinLanguageLevel,
      "--update-baseline",
      "--stacktrace",
      "--quiet",
      "--baseline",
      baselineFilePath.pathString,
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

    // TODO Use reflection to open this
    val main = Main()
    disableDependenciesCheck(main)
    return main.run(args.toTypedArray())
  }


  // A reflection hack for forcing lint to not run against transitive dependencies
  private fun disableDependenciesCheck(lintMain: Main) {
    val mainClass = Class.forName("com.android.tools.lint.Main")
    val lintCliFlagsField = mainClass.getDeclaredField("flags")
    lintCliFlagsField.isAccessible = true

    val lintCliFlags = lintCliFlagsField.get(lintMain) as LintCliFlags
    lintCliFlags.isCheckDependencies = false
  }

  /**
   * Takes a list of AARs and unarchives them into the provided directory
   * with this structure: ${tmpDirectory}/${aarFileName}--aar-unzipped/
   *
   * This is a necessary workaround for Lint wanting to unpack these aars into a global
   * shared directory, which causes lots of obscure concurrency issues inside of lint.
   */
  private fun unpackAars(
    aars: List<Path>,
    dstDirectory: Path,
    executorService: ExecutorService = Executors.newFixedThreadPool(6),
  ): List<Pair<Path, Path>> {
    val aarsToUnpack = aars.map { it to dstDirectory.resolve("${it.name}-aar-unzipped") }
    aarsToUnpack.forEach { (src, dst) -> unzip(src, dst) }
    executorService.awaitTermination(15, TimeUnit.SECONDS)
    return aarsToUnpack.sortedBy { it.first }
  }
}
