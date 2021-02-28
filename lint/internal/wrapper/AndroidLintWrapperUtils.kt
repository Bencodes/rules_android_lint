import com.android.tools.lint.LintCliFlags
import com.android.tools.lint.Main
import java.io.File
import java.nio.file.Path
import java.util.zip.ZipInputStream
import kotlin.io.path.absolutePathString
import kotlin.io.path.inputStream

// By default, Lint uses "$HOME/.android/cache" as shared cache directory for things like unpacking "aar" files.
// This results in concurrency/data issues:
//   - We run lint in parallel in different modules
//   - We use same file names for "aar" files while Lint uses just the file name for its cache dir
// Thus, we override cache dir and make it unique for each build target at the cost of unzipping same "aar" files multiple times during the build.
//
// Nothing about this solution is thread safe, but it at least isolates the cache to the worker and the best we
// can do until lint provides better APIs for this
@Suppress("UNCHECKED_CAST")
internal fun updateAndroidSDKCacheDirectory(cacheDirectory: Path) {
  // Get an instance of the
  val pe = Class.forName("java.lang.ProcessEnvironment")
  val getenv = pe.getDeclaredMethod("getenv")
  getenv.isAccessible = true
  val unmodifiableEnvironment = getenv.invoke(null)
  val map = Class.forName("java.util.Collections\$UnmodifiableMap")
  val m = map.getDeclaredField("m")
  m.isAccessible = true

  (m[unmodifiableEnvironment] as MutableMap<String?, String?>)["ANDROID_SDK_CACHE_DIR"] =
    cacheDirectory.absolutePathString()
}

internal fun unzip(src: Path, dst: Path) {
  val dstFile = dst.toFile()
  val bufferedZipInputStream = src.inputStream().buffered()
  ZipInputStream(bufferedZipInputStream).use { zipStream ->
    var zipEntry = zipStream.nextEntry
    while (zipEntry != null) {
      if (zipEntry.isDirectory) {
        File(dstFile, zipEntry.name).mkdirs()
      } else {
        File(dstFile, zipEntry.name).also { it.parentFile.mkdirs() }.outputStream()
          .use { fileOutputStream -> zipStream.copyTo(fileOutputStream) }
      }

      zipStream.closeEntry()
      zipEntry = zipStream.nextEntry
    }
  }
}
