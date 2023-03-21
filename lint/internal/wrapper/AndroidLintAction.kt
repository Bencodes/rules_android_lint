import java.io.PrintStream
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
    val worker = Worker.fromArgs(args, AndroidLintExecutor())
    val exitCode = worker.processRequests()
    exitProcess(exitCode)
  }

  private class AndroidLintExecutor : Worker.WorkRequestCallback {
    override fun processWorkRequest(args: List<String>, printStream: PrintStream): Int {
      val workingDirectory = Files.createTempDirectory("android_lint")

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
}
