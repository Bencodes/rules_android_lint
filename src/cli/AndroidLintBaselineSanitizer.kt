package com.rules.android.lint.cli

import java.io.File

object AndroidLintBaselineSanitizer {

  internal fun getExecRoot(): String {
    val pwd = File(System.getenv("PWD"))
    return "/${pwd.parentFile.name}/${pwd.name}/"
  }

  internal fun sanitize(content: String, execRoot: String? = null): String {
    return content.split("\n").asSequence().map { sanitizeLine(it, execRoot) }.joinToString("\n")
  }

  internal fun sanitizeLine(content: String, execRoot: String? = null): String {
    if (!content.contains("file=\"")) {
      // No need to sanitize lines that don't have absolute paths in them
      return content
    }

    return content.run {
      removeRelativePathPrefixes(this)
    }.run {
      removeUsingExecRootRoot(execRoot ?: getExecRoot(), this)
    }
  }

  private fun removeRelativePathPrefixes(content: String): String {
    var c = content
    val find = "file=\"../"
    while (c.contains(find)) {
      c = c.replace(find, "file=\"")
    }
    return c
  }

  private fun removeUsingExecRootRoot(execRoot: String, content: String): String {
    if (!content.contains(execRoot)) return content

    val startMarker = "file=\""
    val start = content.indexOf("file=\"") + startMarker.length
    val end = content.indexOf(execRoot) + execRoot.length
    return content.removeRange(start, end)
  }
}
