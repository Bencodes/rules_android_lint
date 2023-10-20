package com.rules.android.lint.cli

import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class AndroidLintBaselineSanitizerTest {

  @Test
  fun `integration tests`() {
    val updateContent = AndroidLintBaselineSanitizer.sanitize(
      execRoot = "/execroot/_main/",
      content = """
<?xml version="1.0" encoding="UTF-8"?>
<issues format="6" by="lint 8.3.0-alpha09">

    <issue
        id="DefaultLocale"
        severity="Error"
        message="Implicitly using the default locale is a common source of bugs: Use `toUpperCase(Locale)` instead. For strings meant to be internal use `Locale.ROOT`, otherwise `Locale.getDefault()`."
        category="Correctness"
        priority="6"
        summary="Implied default locale in case conversion"
        explanation="Calling `String#toLowerCase()` or `#toUpperCase()` **without specifying an explicit locale** is a common source of bugs. The reason for that is that those methods will use the current locale on the user&apos;s device, and even though the code appears to work correctly when you are developing the app, it will fail in some locales. For example, in the Turkish locale, the uppercase replacement for `i` is **not** `I`.&#xA;&#xA;If you want the methods to just perform ASCII replacement, for example to convert an enum name, call `String#toUpperCase(Locale.US)` instead. If you really want to use the current locale, call `String#toUpperCase(Locale.getDefault())` instead."
        url="https://developer.android.com/reference/java/util/Locale.html#default_locale"
        urls="https://developer.android.com/reference/java/util/Locale.html#default_locale"
        errorLine1="    System.out.println(&quot;WRONG&quot;.toUpperCase());"
        errorLine2="                               ~~~~~~~~~~~">
        <location
            file="../../home/runner/.cache/bazel/_bazel_runner/7a23db770589e4e4e3089897b768a899/execroot/_main/Foo.java"
            line="6"
            column="32"/>
    </issue>

    <issue
        id="DefaultLocale"
        severity="Error"
        message="Implicitly using the default locale is a common source of bugs: Use `toUpperCase(Locale)` instead. For strings meant to be internal use `Locale.ROOT`, otherwise `Locale.getDefault()`."
        category="Correctness"
        priority="6"
        summary="Implied default locale in case conversion"
        explanation="Calling `String#toLowerCase()` or `#toUpperCase()` **without specifying an explicit locale** is a common source of bugs. The reason for that is that those methods will use the current locale on the user&apos;s device, and even though the code appears to work correctly when you are developing the app, it will fail in some locales. For example, in the Turkish locale, the uppercase replacement for `i` is **not** `I`.&#xA;&#xA;If you want the methods to just perform ASCII replacement, for example to convert an enum name, call `String#toUpperCase(Locale.US)` instead. If you really want to use the current locale, call `String#toUpperCase(Locale.getDefault())` instead."
        url="https://developer.android.com/reference/java/util/Locale.html#default_locale"
        urls="https://developer.android.com/reference/java/util/Locale.html#default_locale"
        errorLine1="    System.out.println(&quot;WRONG&quot;.toUpperCase());"
        errorLine2="                               ~~~~~~~~~~~">
        <location
            file="private/var/tmp/_bazel_blee/7e5487fa8c60e7043fb2e6c6df8e3854/execroot/_main/Foo.java"
            line="6"
            column="32"/>
    </issue>
</issues>
      """.trimIndent(),
    )

    assertThat(updateContent).isEqualTo(
      """
<?xml version="1.0" encoding="UTF-8"?>
<issues format="6" by="lint 8.3.0-alpha09">

    <issue
        id="DefaultLocale"
        severity="Error"
        message="Implicitly using the default locale is a common source of bugs: Use `toUpperCase(Locale)` instead. For strings meant to be internal use `Locale.ROOT`, otherwise `Locale.getDefault()`."
        category="Correctness"
        priority="6"
        summary="Implied default locale in case conversion"
        explanation="Calling `String#toLowerCase()` or `#toUpperCase()` **without specifying an explicit locale** is a common source of bugs. The reason for that is that those methods will use the current locale on the user&apos;s device, and even though the code appears to work correctly when you are developing the app, it will fail in some locales. For example, in the Turkish locale, the uppercase replacement for `i` is **not** `I`.&#xA;&#xA;If you want the methods to just perform ASCII replacement, for example to convert an enum name, call `String#toUpperCase(Locale.US)` instead. If you really want to use the current locale, call `String#toUpperCase(Locale.getDefault())` instead."
        url="https://developer.android.com/reference/java/util/Locale.html#default_locale"
        urls="https://developer.android.com/reference/java/util/Locale.html#default_locale"
        errorLine1="    System.out.println(&quot;WRONG&quot;.toUpperCase());"
        errorLine2="                               ~~~~~~~~~~~">
        <location
            file="Foo.java"
            line="6"
            column="32"/>
    </issue>

    <issue
        id="DefaultLocale"
        severity="Error"
        message="Implicitly using the default locale is a common source of bugs: Use `toUpperCase(Locale)` instead. For strings meant to be internal use `Locale.ROOT`, otherwise `Locale.getDefault()`."
        category="Correctness"
        priority="6"
        summary="Implied default locale in case conversion"
        explanation="Calling `String#toLowerCase()` or `#toUpperCase()` **without specifying an explicit locale** is a common source of bugs. The reason for that is that those methods will use the current locale on the user&apos;s device, and even though the code appears to work correctly when you are developing the app, it will fail in some locales. For example, in the Turkish locale, the uppercase replacement for `i` is **not** `I`.&#xA;&#xA;If you want the methods to just perform ASCII replacement, for example to convert an enum name, call `String#toUpperCase(Locale.US)` instead. If you really want to use the current locale, call `String#toUpperCase(Locale.getDefault())` instead."
        url="https://developer.android.com/reference/java/util/Locale.html#default_locale"
        urls="https://developer.android.com/reference/java/util/Locale.html#default_locale"
        errorLine1="    System.out.println(&quot;WRONG&quot;.toUpperCase());"
        errorLine2="                               ~~~~~~~~~~~">
        <location
            file="Foo.java"
            line="6"
            column="32"/>
    </issue>
</issues>
      """.trimIndent(),
    )
  }

  @Test
  fun `does not modify local source files that are already in their relative path format`() {
    val updateContent = AndroidLintBaselineSanitizer.sanitizeLine(
      execRoot = "/execroot/_main/",
      content = "file=\"src/main/java/com/example/android/Foo.kt\"",
    )
    assertThat(updateContent).isEqualTo(
      "file=\"src/main/java/com/example/android/Foo.kt\"",
    )
  }

  @Test
  fun `does sanitize files that have relative dot paths`() {
    val updateContent = AndroidLintBaselineSanitizer.sanitizeLine(
      execRoot = "/execroot/_main/",
      content = "file=\"../../../../../src/main/java/com/example/android/Foo.kt\"",
    )
    assertThat(updateContent).isEqualTo(
      "file=\"src/main/java/com/example/android/Foo.kt\"",
    )
  }

  @Test
  fun `does sanitize files based on execroot directory`() {
    val updateContent = AndroidLintBaselineSanitizer.sanitizeLine(
      execRoot = "/execroot/_main/",
      content = "file=\"../../home/runner/.cache/bazel/_bazel_runner/" +
        "7a23db770589e4e4e3089897b768a899/execroot/_main/Foo.java\"",
    )
    assertThat(updateContent).isEqualTo(
      "file=\"Foo.java\"",
    )
  }
}
