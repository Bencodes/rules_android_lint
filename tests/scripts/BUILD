load("@bazel_binaries//:defs.bzl", "bazel_binaries")
load("@rules_bazel_integration_test//bazel_integration_test:defs.bzl", "script_test")
load("@rules_shell//shell:sh_library.bzl", "sh_library")

# Copies the compose-lint-checks jar to a stable runfiles path so the helper can
# rlocation it without depending on rules_jvm_external's repository layout.
genrule(
    name = "compose_lint_checks_jar",
    testonly = True,
    srcs = ["@rules_android_lint_test_deps//:com_slack_lint_compose_compose_lint_checks"],
    outs = ["compose_lint_checks.jar"],
    cmd = "cp $$(echo '$(SRCS)' | tr ' ' '\\n' | grep 'compose-lint-checks' | head -1) $@",
)

sh_library(
    name = "lint_helper",
    testonly = True,
    srcs = ["lint_helper.sh"],
    data = [
        ":compose_lint_checks_jar",
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

script_test(
    name = "android_lint_aar_scenarios_test",
    size = "enormous",
    srcs = ["android_lint_aar_scenarios_test.sh"],
    bazel_binaries = bazel_binaries,
    bazel_version = bazel_binaries.versions.current,
    env_inherit = ["ANDROID_HOME"],
    tags = ["exclusive"],
    deps = [
        ":lint_helper",
        "//tests/bashunit",
    ],
)
