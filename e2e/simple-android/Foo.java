package com.rules.android.lint.examples;

public class Foo {
  public void printWrong() {
    // https://cs.android.com/android-studio/platform/tools/base/+/mirror-goog-studio-main:lint/libs/lint-tests/src/test/java/com/android/tools/lint/checks/LocaleDetectorTest.kt;l=76
    System.out.println("WRONG".toUpperCase());
  }
}
