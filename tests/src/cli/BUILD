load("@rules_kotlin//kotlin:jvm.bzl", "kt_jvm_test")
load("@rules_kotlin//kotlin:lint.bzl", "ktlint_fix", "ktlint_test")
load("//rules:defs.bzl", "android_lint_test")

kt_jvm_test(
    name = "AndroidLintActionArgsTest",
    srcs = ["AndroidLintActionArgsTest.kt"],
    associates = ["//src/cli:lint"],
    test_class = "com.rules.android.lint.cli.AndroidLintActionArgsTest",
    deps = [
        "@rules_android_lint_test_deps//:junit_junit",
        "@rules_android_lint_test_deps//:org_assertj_assertj_core",
    ],
)

android_lint_test(
    name = "AndroidLintActionArgsTest_lint_test",
    srcs = ["AndroidLintActionArgsTest.kt"],
    lib = ":AndroidLintActionArgsTest",
    visibility = ["//visibility:private"],
)

kt_jvm_test(
    name = "AndroidLintActionTest",
    srcs = ["AndroidLintActionTest.kt"],
    associates = ["//src/cli:lint"],
    test_class = "com.rules.android.lint.cli.AndroidLintActionTest",
    deps = [
        "//tests/src/cli/stubs",
        "@rules_android_lint_test_deps//:junit_junit",
        "@rules_android_lint_test_deps//:org_assertj_assertj_core",
    ],
)

kt_jvm_test(
    name = "AndroidLintCliInvokerCacheTest",
    srcs = ["AndroidLintCliInvokerCacheTest.kt"],
    associates = ["//src/cli:lint"],
    test_class = "com.rules.android.lint.cli.AndroidLintCliInvokerCacheTest",
    deps = [
        "//tests/src/cli/stubs",
        "@rules_android_lint_test_deps//:junit_junit",
        "@rules_android_lint_test_deps//:org_assertj_assertj_core",
    ],
)

android_lint_test(
    name = "AndroidLintActionTest_lint_test",
    srcs = ["AndroidLintActionTest.kt"],
    lib = ":AndroidLintActionTest",
    visibility = ["//visibility:private"],
)

kt_jvm_test(
    name = "AndroidLintProjectTest",
    srcs = ["AndroidLintProjectTest.kt"],
    associates = ["//src/cli:lint"],
    test_class = "com.rules.android.lint.cli.AndroidLintProjectTest",
    deps = [
        "@rules_android_lint_test_deps//:junit_junit",
        "@rules_android_lint_test_deps//:org_assertj_assertj_core",
    ],
)

kt_jvm_test(
    name = "AndroidLintRunnerTest",
    srcs = ["AndroidLintRunnerTest.kt"],
    associates = ["//src/cli:lint"],
    test_class = "com.rules.android.lint.cli.AndroidLintRunnerTest",
    deps = [
        "//tests/src/cli/stubs",
        "@rules_android_lint_test_deps//:junit_junit",
        "@rules_android_lint_test_deps//:org_assertj_assertj_core",
    ],
)

android_lint_test(
    name = "AndroidLintProjectTest_lint_test",
    srcs = ["AndroidLintProjectTest.kt"],
    lib = ":AndroidLintProjectTest",
    visibility = ["//visibility:private"],
)

android_lint_test(
    name = "AndroidLintRunnerTest_lint_test",
    srcs = ["AndroidLintRunnerTest.kt"],
    lib = ":AndroidLintRunnerTest",
    visibility = ["//visibility:private"],
)

ktlint_test(
    name = "lint_ktlint_test",
    srcs = glob(["*.kt"]),
    config = "//:editorconfig",
    visibility = ["//visibility:private"],
)

ktlint_fix(
    name = "lint_ktlint_fix",
    srcs = glob(["*.kt"]),
    config = "//:editorconfig",
    visibility = ["//visibility:private"],
)

test_suite(
    name = "tests",
    tests = [
        ":AndroidLintActionArgsTest",
        ":AndroidLintActionTest",
        ":AndroidLintCliInvokerCacheTest",
        ":AndroidLintProjectTest",
        ":AndroidLintRunnerTest",
    ],
)
