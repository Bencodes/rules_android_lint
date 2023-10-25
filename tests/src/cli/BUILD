load("@rules_kotlin//kotlin:jvm.bzl", "kt_jvm_test")
load("@rules_kotlin//kotlin:lint.bzl", "ktlint_fix", "ktlint_test")
load("//rules:defs.bzl", "android_lint_test")

kt_jvm_test(
    name = "AndroidLintActionArgsTest",
    srcs = ["AndroidLintActionArgsTest.kt"],
    associates = ["//src/cli:lint"],
    test_class = "com.rules.android.lint.cli.AndroidLintActionArgsTest",
    deps = [
        "@rules_android_lint_deps//:junit_junit",
        "@rules_android_lint_deps//:org_assertj_assertj_core",
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
        "@rules_android_lint_deps//:junit_junit",
        "@rules_android_lint_deps//:org_assertj_assertj_core",
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
        "@rules_android_lint_deps//:junit_junit",
        "@rules_android_lint_deps//:org_assertj_assertj_core",
    ],
)

android_lint_test(
    name = "AndroidLintProjectTest_lint_test",
    srcs = ["AndroidLintProjectTest.kt"],
    lib = ":AndroidLintProjectTest",
    visibility = ["//visibility:private"],
)

kt_jvm_test(
    name = "AndroidLintBaselineSanitizerTest",
    srcs = ["AndroidLintBaselineSanitizerTest.kt"],
    associates = ["//src/cli:lint"],
    test_class = "com.rules.android.lint.cli.AndroidLintBaselineSanitizerTest",
    deps = [
        "@rules_android_lint_deps//:junit_junit",
        "@rules_android_lint_deps//:org_assertj_assertj_core",
    ],
)

android_lint_test(
    name = "AndroidLintBaselineSanitizerTest_lint_test",
    srcs = ["AndroidLintBaselineSanitizerTest.kt"],
    lib = ":AndroidLintBaselineSanitizerTest",
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
        ":AndroidLintBaselineSanitizerTest",
        ":AndroidLintProjectTest",
    ],
)
