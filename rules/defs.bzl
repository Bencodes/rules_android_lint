"""Android Lint rules
"""

load(":android_lint.bzl", _android_lint = "android_lint")
load(":android_lint_test.bzl", _android_lint_test = "android_lint_test")
load(":providers.bzl", _AndroidLintResultsInfo = "AndroidLintResultsInfo")

android_lint = _android_lint
android_lint_test = _android_lint_test
AndroidLintResultsInfo = _AndroidLintResultsInfo
