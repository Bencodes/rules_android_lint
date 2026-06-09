load("@bazel_binaries//:defs.bzl", "bazel_binaries")
load("@rules_bazel_integration_test//bazel_integration_test:defs.bzl", "script_test")
load("@rules_shell//shell:sh_library.bzl", "sh_library")

sh_library(
    name = "lint_helper",
    testonly = True,
    srcs = ["lint_helper.sh"],
    data = [
        "//rules:all_files",
        "//src:all_files",
        "//src/cli:cli_deploy.jar",
        "//third_party:com_android_tools_lint_lint_deploy.jar",
        "//toolchains:all_files",
    ],
)

script_test(
    name = "android_lint_scenarios_test",
    size = "enormous",
    srcs = ["android_lint_scenarios_test.sh"],
    bazel_binaries = bazel_binaries,
    bazel_version = bazel_binaries.versions.current,
    tags = ["exclusive"],
    deps = [
        ":lint_helper",
        "//tests/bashunit",
    ],
)
