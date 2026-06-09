package com.android.tools.lint

import java.util.concurrent.atomic.AtomicInteger

/** Test stub recording disposal calls made by [AndroidLintCliInvoker.dispose]. */
class UastEnvironment {
  companion object {
    val disposeCount = AtomicInteger(0)

    @JvmStatic
    fun disposeApplicationEnvironment() {
      disposeCount.incrementAndGet()
    }
  }
}
