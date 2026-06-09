load("@rules_kotlin//kotlin:jvm.bzl", "kt_jvm_library")

kt_jvm_library(
    name = "stubs",
    testonly = True,
    srcs = glob(["*.kt"]),
    visibility = ["//tests/src/cli:__pkg__"],
)
