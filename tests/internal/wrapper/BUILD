load("@io_bazel_rules_kotlin//kotlin:jvm.bzl", "kt_jvm_library", "kt_jvm_test")

kt_jvm_test(
    name = "AndroidLintActionTest",
    srcs = ["AndroidLintActionTest.kt"],
    associates = ["//lint/internal/wrapper:lint"],
    test_class = "AndroidLintActionTest",
    deps = [
        "@rules_android_lint_dependencies//:junit_junit",
        "@rules_android_lint_dependencies//:org_assertj_assertj_core",
    ],
)

kt_jvm_test(
    name = "AndroidLintProjectTest",
    srcs = ["AndroidLintProjectTest.kt"],
    associates = ["//lint/internal/wrapper:lint"],
    test_class = "AndroidLintProjectTest",
    deps = [
        "@rules_android_lint_dependencies//:junit_junit",
        "@rules_android_lint_dependencies//:org_assertj_assertj_core",
    ],
)

kt_jvm_test(
    name = "AndroidLintActionArgsTest",
    srcs = ["AndroidLintActionArgsTest.kt"],
    associates = ["//lint/internal/wrapper:lint"],
    test_class = "AndroidLintActionArgsTest",
    deps = [
        "@rules_android_lint_dependencies//:junit_junit",
        "@rules_android_lint_dependencies//:org_assertj_assertj_core",
    ],
)

test_suite(
    name = "tests",
    tests = [
        ":AndroidLintActionArgsTest",
        ":AndroidLintActionTest",
        ":AndroidLintProjectTest",
    ],
)
