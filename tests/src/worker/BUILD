load("@rules_kotlin//kotlin:jvm.bzl", "kt_jvm_test")
load("//rules:defs.bzl", "android_lint_test")
load("@rules_kotlin//kotlin:lint.bzl", "ktlint_fix", "ktlint_test")

kt_jvm_test(
    name = "WorkerTest",
    srcs = ["WorkerTest.kt"],
    associates = ["//src/worker"],
    test_class = "com.rules.android.lint.worker.WorkerTest",
    deps = [
        "@rules_android_lint_deps//:junit_junit",
        "@rules_android_lint_deps//:org_assertj_assertj_core",
    ],
)

android_lint_test(
    name = "WorkerTest_lint_test",
    srcs = ["WorkerTest.kt"],
    lib = ":WorkerTest",
    visibility = ["//visibility:private"],
)

ktlint_test(
    name = "tests_ktlint_test",
    srcs = glob(["*.kt"]),
    config = "//:editorconfig",
    visibility = ["//visibility:private"],
)

ktlint_fix(
    name = "tests_ktlint_fix",
    srcs = glob(["*.kt"]),
    config = "//:editorconfig",
    visibility = ["//visibility:private"],
)

test_suite(
    name = "tests",
    tests = [
        ":WorkerTest",
        ":tests_ktlint_test",
    ],
)
