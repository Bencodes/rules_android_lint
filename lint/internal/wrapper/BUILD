load("@rules_java//java:defs.bzl", "java_binary")
load("@io_bazel_rules_kotlin//kotlin:jvm.bzl", "kt_jvm_library")

java_binary(
    name = "lint_wrapper",
    main_class = "AndroidLintAction",
    visibility = ["//visibility:public"],
    runtime_deps = [":lint"],
)

kt_jvm_library(
    name = "lint",
    srcs = glob(["*.kt"]),
    visibility = ["//visibility:public"],
    deps = [
        "//lint/internal/worker",
        "@rules_android_lint_dependencies//:com_android_tools_lint_lint",
        "@rules_android_lint_dependencies//:com_android_tools_lint_lint_api",
        "@rules_android_lint_dependencies//:com_android_tools_lint_lint_checks",
        "@rules_android_lint_dependencies//:com_android_tools_lint_lint_model",
        "@rules_android_lint_dependencies//:com_xenomachina_kotlin_argparser",
    ],
)
