load("@rules_kotlin//kotlin:jvm.bzl", "kt_jvm_library")
load("@rules_kotlin//kotlin:lint.bzl", "ktlint_fix", "ktlint_test")
load("//rules:defs.bzl", "android_lint_test")

kt_jvm_library(
    name = "worker",
    srcs = glob(["*.kt"]),
    visibility = ["//visibility:public"],
    deps = [
        "@rules_android_lint_deps//:com_squareup_moshi_moshi",
        "@rules_android_lint_deps//:com_squareup_moshi_moshi_kotlin",
        "@rules_android_lint_deps//:com_squareup_okio_okio_jvm",
        "@rules_android_lint_deps//:io_reactivex_rxjava3_rxjava",
        "@rules_android_lint_deps//:org_reactivestreams_reactive_streams",
        "@rules_kotlin//src/main/kotlin:worker",
    ],
)

ktlint_test(
    name = "worker_ktlint_test",
    srcs = glob(["*.kt"]),
    config = "//:editorconfig",
    visibility = ["//visibility:private"],
)

ktlint_fix(
    name = "worker_ktlint_fix",
    srcs = glob(["*.kt"]),
    config = "//:editorconfig",
    visibility = ["//visibility:private"],
)

android_lint_test(
    name = "worker_lint_test",
    srcs = glob(["*.kt"]),
    lib = ":worker",
    visibility = ["//visibility:private"],
)
