package com.rules.android.lint.cli

class AndroidLintCliInvoker(
  classLoader: ClassLoader,
) {

  private val mainClass = Class.forName("com.android.tools.lint.Main")
  private val cliFlags = classLoader.loadClass("com.android.tools.lint.LintCliFlags")
  private val main = Class.forName("com.android.tools.lint.Main").getDeclaredConstructor()
    .newInstance()
  private val flagsInstance = mainClass.getDeclaredField("flags").apply {
    isAccessible = true
  }.get(main)

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
    return runMethod.invoke(main, args) as Int
  }

  fun setCheckDependencies(enableCheckDependencies: Boolean) {
    val setCheckDependenciesMethod = cliFlags.getDeclaredMethod(
      "setCheckDependencies",
      Boolean::class.java,
    )
    setCheckDependenciesMethod.invoke(flagsInstance, enableCheckDependencies)
  }

  companion object {

    val ERRNO_SUCCESS = 0
    val ERRNO_ERRORS = 1
    val ERRNO_USAGE = 2
    val ERRNO_EXISTS = 3
    val ERRNO_HELP = 4
    val ERRNO_INVALID_ARGS = 5
    val ERRNO_CREATED_BASELINE = 6
    val ERRNO_APPLIED_SUGGESTIONS = 7
    val ERRNO_INTERNAL_CONTINUE = 100
  }
}
