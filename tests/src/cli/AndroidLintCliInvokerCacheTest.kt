package com.rules.android.lint.cli

import com.android.tools.lint.Main
import com.android.tools.lint.UastEnvironment
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.attribute.FileTime
import java.util.concurrent.CyclicBarrier
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.jar.JarEntry
import java.util.jar.JarOutputStream

@RunWith(JUnit4::class)
class AndroidLintCliInvokerCacheTest {
  @get:Rule
  val temporaryFolder = TemporaryFolder()

  @Before
  fun setUp() {
    Main.reset()
  }

  @Test
  fun `acquiring twice with an unchanged jar reuses the invoker`() {
    val cache = AndroidLintCliInvokerCache()
    val jar = writeJar("lint.jar", entryCount = 1)

    val first = cache.acquire(listOf(jar))
    cache.release(first)
    val second = cache.acquire(listOf(jar))
    cache.release(second)

    assertThat(second).isSameAs(first)
    assertThat(cache.createdCount).isEqualTo(1)
  }

  @Test
  fun `a changed jar evicts the cached invoker and disposes it`() {
    val cache = AndroidLintCliInvokerCache()
    val jar = writeJar("lint.jar", entryCount = 1)

    val first = cache.acquire(listOf(jar))
    cache.release(first)

    val disposesBefore = UastEnvironment.disposeCount.get()
    rewriteJar(jar, entryCount = 2)
    val second = cache.acquire(listOf(jar))
    cache.release(second)

    assertThat(second).isNotSameAs(first)
    assertThat(cache.createdCount).isEqualTo(2)
    assertThat(UastEnvironment.disposeCount.get()).isEqualTo(disposesBefore + 1)
  }

  @Test
  fun `eviction defers disposal until the last lease is released`() {
    val cache = AndroidLintCliInvokerCache()
    val jar = writeJar("lint.jar", entryCount = 1)

    val first = cache.acquire(listOf(jar))

    val disposesBefore = UastEnvironment.disposeCount.get()
    rewriteJar(jar, entryCount = 2)
    val second = cache.acquire(listOf(jar))

    // First invoker is evicted but still leased, so it must not be disposed yet.
    assertThat(UastEnvironment.disposeCount.get()).isEqualTo(disposesBefore)

    cache.release(first)
    assertThat(UastEnvironment.disposeCount.get()).isEqualTo(disposesBefore + 1)

    cache.release(second)
    assertThat(UastEnvironment.disposeCount.get()).isEqualTo(disposesBefore + 1)
  }

  @Test
  fun `invoke creates a fresh Main per call and applies checkDependencies per call`() {
    val cache = AndroidLintCliInvokerCache()
    val jar = writeJar("lint.jar", entryCount = 1)
    val invoker = cache.acquire(listOf(jar))

    val exitCode = invoker.invoke(args = arrayOf("--first"), enableCheckDependencies = true)
    invoker.invoke(args = arrayOf("--second"), enableCheckDependencies = false)
    cache.release(invoker)

    assertThat(exitCode).isEqualTo(AndroidLintCliInvoker.ERRNO_SUCCESS)
    assertThat(Main.recordedRuns).hasSize(2)
    val (firstRun, secondRun) = Main.recordedRuns
    assertThat(firstRun.args).containsExactly("--first")
    assertThat(firstRun.checkDependencies).isTrue()
    assertThat(secondRun.args).containsExactly("--second")
    assertThat(secondRun.checkDependencies).isFalse()
    assertThat(firstRun.instance).isNotSameAs(secondRun.instance)
  }

  @Test
  fun `concurrent invocations share one classloader with independent Main instances`() {
    val cache = AndroidLintCliInvokerCache()
    val jar = writeJar("lint.jar", entryCount = 1)
    // Both runs must be in flight at the same time before either completes.
    Main.runBarrier = CyclicBarrier(2)

    val executor = Executors.newFixedThreadPool(2)
    try {
      val futures =
        (1..2).map { index ->
          executor.submit<Int> {
            val invoker = cache.acquire(listOf(jar))
            try {
              invoker.invoke(args = arrayOf("--request-$index"), enableCheckDependencies = false)
            } finally {
              cache.release(invoker)
            }
          }
        }
      futures.forEach { assertThat(it.get(10, TimeUnit.SECONDS)).isEqualTo(0) }
    } finally {
      executor.shutdownNow()
    }

    assertThat(cache.createdCount).isEqualTo(1)
    assertThat(Main.recordedRuns).hasSize(2)
    assertThat(Main.recordedRuns[0].instance).isNotSameAs(Main.recordedRuns[1].instance)
  }

  private fun writeJar(
    name: String,
    entryCount: Int,
  ): Path {
    val jar = temporaryFolder.newFile(name).toPath()
    rewriteJar(jar, entryCount)
    return jar
  }

  private fun rewriteJar(
    jar: Path,
    entryCount: Int,
  ) {
    JarOutputStream(Files.newOutputStream(jar)).use { stream ->
      repeat(entryCount) { index ->
        stream.putNextEntry(JarEntry("entry-$index.txt"))
        stream.write("content-$index".toByteArray())
        stream.closeEntry()
      }
    }
    // Force a key change even on filesystems with coarse mtime resolution.
    val currentTime = Files.getLastModifiedTime(jar).toMillis()
    Files.setLastModifiedTime(jar, FileTime.fromMillis(currentTime + 2_000))
  }
}
