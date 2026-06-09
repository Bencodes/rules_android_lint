package com.rules.android.lint.cli

import java.nio.file.Files
import java.nio.file.Path

/**
 * Caches the [AndroidLintCliInvoker] across work requests so a persistent worker pays lint's
 * classloading and UAST application-environment warm-up cost once, not once per request.
 *
 * The cache key is derived from each jar's real path, size, and mtime rather than its content
 * digest: hashing the multi-hundred-megabyte lint deploy jar on every request would claw back
 * much of the win, and a lint version change always changes the jar size in practice.
 *
 * Eviction is lease-aware: a multiplex worker can receive a request built against a new lint jar
 * while a request against the old jar is still running, so the old classloader is only disposed
 * once its last lease is released.
 */
internal class AndroidLintCliInvokerCache {
  private class Entry(
    val key: String,
    val invoker: AndroidLintCliInvoker,
    var activeLeases: Int,
  )

  private var current: Entry? = null
  private val evictedWithLeases = mutableListOf<Entry>()

  /** Number of invokers (classloaders) created over the lifetime of this cache. */
  internal var createdCount: Int = 0
    private set

  @Synchronized
  internal fun acquire(jars: List<Path>): AndroidLintCliInvoker {
    val key = cacheKey(jars)
    current?.let { entry ->
      if (entry.key == key) {
        entry.activeLeases += 1
        return entry.invoker
      }
      if (entry.activeLeases > 0) {
        evictedWithLeases.add(entry)
      } else {
        entry.invoker.dispose()
      }
      current = null
    }
    val invoker = AndroidLintCliInvoker.createUsingJars(jars = jars.toTypedArray())
    createdCount += 1
    current = Entry(key = key, invoker = invoker, activeLeases = 1)
    return invoker
  }

  @Synchronized
  internal fun release(invoker: AndroidLintCliInvoker) {
    current?.let { entry ->
      if (entry.invoker === invoker) {
        entry.activeLeases -= 1
        return
      }
    }
    val iterator = evictedWithLeases.iterator()
    while (iterator.hasNext()) {
      val entry = iterator.next()
      if (entry.invoker === invoker) {
        entry.activeLeases -= 1
        if (entry.activeLeases <= 0) {
          iterator.remove()
          entry.invoker.dispose()
        }
        return
      }
    }
  }

  private fun cacheKey(jars: List<Path>): String =
    jars.joinToString(separator = "|") { jar ->
      val realPath = jar.toRealPath()
      val size = Files.size(realPath)
      val lastModified = Files.getLastModifiedTime(realPath).toMillis()
      "$realPath:$size:$lastModified"
    }
}
