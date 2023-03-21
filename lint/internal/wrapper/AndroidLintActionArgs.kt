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

  val moduleName: String by parser.storing(
    names = arrayOf("--module-name"),
    help = "",
  )

  val label: String by parser.storing(
    names = arrayOf("--label"),
    help = "",
  )

  val srcs: List<Path> by parser.adding(
    names = arrayOf("--src"),
    help = "",
    transform = argsParserPathTransformer,
  )

  val output: Path by parser.storing(
    names = arrayOf("--output"),
    help = "",
    transform = argsParserPathTransformer,
  )

  val projectConfigOutput: Path by parser.storing(
    names = arrayOf("--project-config-output"),
    help = "",
    transform = argsParserPathTransformer,
  )

  val resources: List<Path> by parser.adding(
    names = arrayOf("--resource"),
    help = "",
    transform = argsParserPathTransformer,
  ).default { emptyList() }

  val androidManifest: Path? by parser.storing(
    names = arrayOf("--android-manifest"),
    help = "",
    transform = argsParserPathTransformer,
  ).default { null }

  val moduleRoot: Path by parser.storing(
    names = arrayOf("--module-root"),
    help = "",
    transform = argsParserPathTransformer,
  )

  val baselineFile: Path? by parser.storing(
    names = arrayOf("--baseline-file"),
    help = "",
    transform = argsParserPathTransformer,
  ).default { null }

  val config: Path? by parser.storing(
    names = arrayOf("--config-file"),
    help = "",
    transform = argsParserPathTransformer,
  ).default { null }

  val customChecks: List<Path> by parser.adding(
    names = arrayOf("--custom-rule"),
    help = "",
    transform = argsParserPathTransformer,
  ).default { emptyList() }

  val classpath: List<Path> by parser.adding(
    names = arrayOf("--classpath"),
    help = "",
    transform = argsParserPathTransformer,
  ).default { emptyList() }

  val autofix: Boolean by parser.flagging(
    names = arrayOf("--autofix"),
    help = "",
  ).default { false }

  val regenerateBaselineFile: Boolean by parser.flagging(
    names = arrayOf("--regenerate-baseline-files"),
    help = "",
  ).default { false }

  val warningsAsErrors: Boolean by parser.flagging(
    names = arrayOf("--warnings-as-errors"),
    help = "",
  ).default { false }

  val enableChecks: List<String> by parser.adding(
    names = arrayOf("--enable-check"),
    help = "",
  ).default { emptyList() }

  val disableChecks: List<String> by parser.adding(
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

  companion object {

    internal fun parseArgs(args: List<String>): AndroidLintActionArgs {
      // TODO Need to handle the --flagfile argument here
      val unwrappedArgs: Array<String> = if (args.size == 1 && args[0].startsWith("@")) {
        File(args[0].removePrefix("@")).readLines(Charset.defaultCharset()).toTypedArray()
      } else {
        args.toTypedArray()
      }

      return AndroidLintActionArgs(ArgParser(unwrappedArgs))
    }
  }
}
