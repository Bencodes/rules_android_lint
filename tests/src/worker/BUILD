load("@rules_kotlin//kotlin:jvm.bzl", "kt_jvm_test")
load("//rules:defs.bzl", "android_lint_test")

kt_jvm_test(
    name = "WorkerIOTest",
    srcs = ["WorkerIOTest.kt"],
    associates = ["//src/worker"],
    test_class = "com.rules.android.lint.worker.WorkerIOTest",
    deps = [
        "@rules_android_lint_deps//:junit_junit",
        "@rules_android_lint_deps//:org_assertj_assertj_core",
    ],
)

kt_jvm_test(
    name = "WorkerJsonMessageProcessorTest",
    srcs = ["WorkerJsonMessageProcessorTest.kt"],
    associates = ["//src/worker"],
    test_class = "com.rules.android.lint.worker.WorkerJsonMessageProcessorTest",
    deps = [
        "@rules_android_lint_deps//:junit_junit",
        "@rules_android_lint_deps//:org_assertj_assertj_core",
    ],
)

android_lint_test(
    name = "WorkerIOTest_lint_test",
    srcs = ["WorkerIOTest.kt"],
    lib = ":WorkerIOTest",
    visibility = ["//visibility:private"],
)

android_lint_test(
    name = "WorkerJsonMessageProcessorTest_lint_test",
    srcs = ["WorkerJsonMessageProcessorTest.kt"],
    lib = ":WorkerJsonMessageProcessorTest",
    visibility = ["//visibility:private"],
)

test_suite(
    name = "tests",
    tests = [
        ":WorkerIOTest",
        ":WorkerJsonMessageProcessorTest",
    ],
)
