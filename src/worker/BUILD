load("@rules_kotlin//kotlin:jvm.bzl", "kt_jvm_library")
load("@rules_kotlin//kotlin:lint.bzl", "ktlint_fix", "ktlint_test")
load("//rules:defs.bzl", "android_lint_test")

kt_jvm_library(
    name = "worker",
    srcs = glob(["*.kt"]),
    visibility = ["//visibility:public"],
    deps = [
        "@bazel_worker_java//:worker_protocol_java_proto",
        "@bazel_worker_java//src/main/java/com/google/devtools/build/lib/worker:work_request_handlers",
        "@rules_android_lint_deps//:com_google_protobuf_protobuf_java",
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
