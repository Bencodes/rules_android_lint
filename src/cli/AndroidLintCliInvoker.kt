package com.rules.android.lint.cli

import java.net.URL
import java.net.URLClassLoader
import java.nio.file.Path
import kotlin.io.path.exists
import kotlin.io.path.isRegularFile
import kotlin.io.path.pathString

class AndroidLintCliInvoker(
  classLoader: ClassLoader,
) {
  private val mainClass = Class.forName("com.android.tools.lint.Main", true, classLoader)
  private val cliFlags = classLoader.loadClass("com.android.tools.lint.LintCliFlags")
  private val mainInstance =
    mainClass
      .getDeclaredConstructor()
      .newInstance()
  private val flagsInstance =
    mainClass
      .getDeclaredField("flags")
      .apply {
        isAccessible = true
      }.get(mainInstance)

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
  fun invoke(args: Array<String>): Int {
    val runMethod = mainClass.getDeclaredMethod("run", Array<String>::class.java)
    return runMethod.invoke(mainInstance, args) as Int
  }

  fun setCheckDependencies(enableCheckDependencies: Boolean) {
    val setCheckDependenciesMethod =
      cliFlags.getDeclaredMethod(
        "setCheckDependencies",
        Boolean::class.java,
      )
    setCheckDependenciesMethod.invoke(flagsInstance, enableCheckDependencies)
  }

  companion object {
    const val ERRNO_SUCCESS = 0
    const val ERRNO_ERRORS = 1
    const val ERRNO_USAGE = 2
    const val ERRNO_EXISTS = 3
    const val ERRNO_HELP = 4
    const val ERRNO_INVALID_ARGS = 5
    const val ERRNO_CREATED_BASELINE = 6
    const val ERRNO_APPLIED_SUGGESTIONS = 7
    const val ERRNO_INTERNAL_CONTINUE = 100

    fun createUsingJars(
      parentClassloader: ClassLoader = this::class.java.classLoader,
      vararg jars: Path,
    ): AndroidLintCliInvoker {
      require(jars.isNotEmpty()) {
        "Error: At least one jar must be provided when calling createUsingJars"
      }

      val classpath =
        jars
          .map { jar ->
            require(jar.isRegularFile() && jar.exists()) {
              "Error: The provided jar does not exist!: ${jar.pathString}"
            }
            URL("file:${jar.pathString}")
          }.toTypedArray()

      return AndroidLintCliInvoker(classLoader = URLClassLoader(classpath, parentClassloader))
    }
  }
}
