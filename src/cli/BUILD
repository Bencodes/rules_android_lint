load("@rules_java//java:defs.bzl", "java_binary")
load("@rules_kotlin//kotlin:jvm.bzl", "kt_jvm_library")
load("@rules_kotlin//kotlin:lint.bzl", "ktlint_fix", "ktlint_test")
load("//rules:defs.bzl", "android_lint_test")

java_binary(
    name = "cli",
    main_class = "com.rules.android.lint.cli.AndroidLintAction",
    visibility = ["//visibility:public"],
    runtime_deps = [":lint"],
)

kt_jvm_library(
    name = "lint",
    srcs = glob(["*.kt"]),
    visibility = ["//visibility:public"],
    deps = [
        "//src/worker",
        "@rules_android_lint_deps//:com_xenomachina_kotlin_argparser",
    ],
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

android_lint_test(
    name = "lint_lint_test",
    srcs = glob(["*.kt"]),
    lib = ":lint",
    visibility = ["//visibility:private"],
)
