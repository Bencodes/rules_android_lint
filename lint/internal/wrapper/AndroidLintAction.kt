import java.nio.file.Files
import kotlin.system.exitProcess

object AndroidLintAction {

  init {
    // Attempt to better isolate the Android lint execution
    val androidSdkCacheDirectory = Files.createTempDirectory("android_sdk_cache")
    androidSdkCacheDirectory.toFile().deleteOnExit()
    updateAndroidSDKCacheDirectory(androidSdkCacheDirectory)
  }

  @JvmStatic
  fun main(args: Array<String>) {
    if (args.size == 1 && args.contains("--persistent_worker")) {
      runPersistentWorker()
    } else {
      val exitCode = processRequest(args)
      exitProcess(exitCode)
    }
  }

  private fun runPersistentWorker(): Unit =
    throw IllegalArgumentException("Persistent worker support is not ready!")

  private fun processRequest(args: Array<String>): Int {
    val workingDirectory =
      Files.createTempDirectory("android_lint")

    try {
      val runner = AndroidLintRunner()
      val parsedArgs = AndroidLintActionArgs.parseArgs(args)
      runner.runAndroidLint(parsedArgs, workingDirectory)
      return 0
    } catch (exception: Exception) {
      exception.printStackTrace()
      return 1
    } finally {
      try {
        workingDirectory.toFile().deleteRecursively()
      } catch (e: Exception) {
        e.printStackTrace()
      }
    }
  }
}
