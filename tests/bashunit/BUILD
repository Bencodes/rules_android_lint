# Vendored from rules_android's test/bashunit (Apache 2.0), which itself vendors
# Bazel's unittest.bash.
load("@rules_shell//shell:sh_library.bzl", "sh_library")

sh_library(
    name = "bashunit",
    testonly = True,
    srcs = [
        "unittest.bash",
        "unittest_utils.sh",
    ],
    visibility = ["//tests:__subpackages__"],
)
