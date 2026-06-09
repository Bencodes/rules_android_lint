package com.rules.android.lint.cli

import java.lang.reflect.InvocationTargetException
import java.net.URL
import java.net.URLClassLoader
import java.nio.file.Path
import kotlin.io.path.exists
import kotlin.io.path.isRegularFile
import kotlin.io.path.pathString

class AndroidLintCliInvoker(
  internal val classLoader: ClassLoader,
) {
  private val mainClass = Class.forName("com.android.tools.lint.Main", true, classLoader)
  private val cliFlagsClass = classLoader.loadClass("com.android.tools.lint.LintCliFlags")
  private val runMethod = mainClass.getDeclaredMethod("run", Array<String>::class.java)
  private val flagsField =
    mainClass
      .getDeclaredField("flags")
      .apply { isAccessible = true }
  private val setCheckDependenciesMethod =
    cliFlagsClass.getDeclaredMethod("setCheckDependencies", Boolean::class.java)

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
  fun invoke(
    args: Array<String>,
    enableCheckDependencies: Boolean = false,
  ): Int {
    // A fresh Main per invocation: Main carries per-run flag state, and the cached invoker may
    // serve concurrent work requests in a multiplex worker.
    val mainInstance = mainClass.getDeclaredConstructor().newInstance()
    setCheckDependenciesMethod.invoke(flagsField.get(mainInstance), enableCheckDependencies)

    // Null out the context classloader while lint runs so service loading inside lint cannot
    // resolve against the worker's own classpath.
    val previousContextClassLoader = Thread.currentThread().contextClassLoader
    return try {
      Thread.currentThread().contextClassLoader = null
      runMethod.invoke(mainInstance, args) as Int
    } catch (exception: InvocationTargetException) {
      throw exception.targetException
    } finally {
      Thread.currentThread().contextClassLoader = previousContextClassLoader
    }
  }

  /**
   * Releases the resources held by this invoker's classloader.
   *
   * Lint keeps a JVM-wide IntelliJ application environment alive per classloader. Without
   * disposing it, every evicted classloader leaks metaspace for the lifetime of the worker.
   */
  fun dispose() {
    try {
      classLoader
        .loadClass("com.android.tools.lint.UastEnvironment")
        .getDeclaredMethod("disposeApplicationEnvironment")
        .invoke(null)
    } catch (exception: ReflectiveOperationException) {
      // Best-effort cleanup; never fail eviction because the jar lacks UastEnvironment.
    } finally {
      (classLoader as? URLClassLoader)?.close()
    }
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
