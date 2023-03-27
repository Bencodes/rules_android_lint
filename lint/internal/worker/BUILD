load("@io_bazel_rules_kotlin//kotlin:jvm.bzl", "kt_jvm_library")

kt_jvm_library(
    name = "worker",
    srcs = glob(["*.kt"]),
    visibility = ["//visibility:public"],
    deps = [
        "@rules_android_lint_dependencies//:com_squareup_moshi_moshi",
        "@rules_android_lint_dependencies//:com_squareup_moshi_moshi_kotlin",
        "@rules_android_lint_dependencies//:com_squareup_okio_okio_jvm",
        "@rules_android_lint_dependencies//:io_reactivex_rxjava3_rxjava",
        "@rules_android_lint_dependencies//:org_reactivestreams_reactive_streams",
    ],
)
