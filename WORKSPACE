workspace(name = "rules_android_lint")

load("@bazel_tools//tools/build_defs/repo:http.bzl", "http_archive")

# ## Lint Dependencies

load("@rules_android_lint//lint:dependencies.bzl", "rules_android_lint_dependencies")

rules_android_lint_dependencies()

load("@rules_android_lint//lint:toolchains.bzl", "rules_android_lint_toolchains")

rules_android_lint_toolchains()

## Lint Testing

http_archive(
    name = "bazel_skylib",
    sha256 = "1c531376ac7e5a180e0237938a2536de0c54d93f5c278634818e0efc952dd56c",
    url = "https://github.com/bazelbuild/bazel-skylib/releases/download/1.0.3/bazel-skylib-1.0.3.tar.gz",
)

load("@bazel_skylib//:workspace.bzl", "bazel_skylib_workspace")

bazel_skylib_workspace()

# Kotlin

http_archive(
    name = "io_bazel_rules_kotlin",
    sha256 = "fd92a98bd8a8f0e1cdcb490b93f5acef1f1727ed992571232d33de42395ca9b3",
    url = "https://github.com/bazelbuild/rules_kotlin/releases/download/v1.7.1/rules_kotlin_release.tgz",
)

load("@io_bazel_rules_kotlin//kotlin:repositories.bzl", "kotlin_repositories")

kotlin_repositories()

load("@io_bazel_rules_kotlin//kotlin:core.bzl", "kt_register_toolchains")

kt_register_toolchains()

## Buildifier

http_archive(
    name = "buildifier_prebuilt",
    sha256 = "f7093a960a8c3471552764892ce12cb62d9b72600fa4c8b08b2090c45db05ce8",
    strip_prefix = "buildifier-prebuilt-6.0.0.1",
    url = "http://github.com/keith/buildifier-prebuilt/archive/6.0.0.1.tar.gz",
)

load("@buildifier_prebuilt//:deps.bzl", "buildifier_prebuilt_deps")

buildifier_prebuilt_deps()

load("@buildifier_prebuilt//:defs.bzl", "buildifier_prebuilt_register_toolchains")

buildifier_prebuilt_register_toolchains()
