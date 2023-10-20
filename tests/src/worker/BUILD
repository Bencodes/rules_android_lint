load("@rules_kotlin//kotlin:jvm.bzl", "kt_jvm_test")

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

test_suite(
    name = "tests",
    tests = [
        ":WorkerIOTest",
        ":WorkerJsonMessageProcessorTest",
    ],
)
