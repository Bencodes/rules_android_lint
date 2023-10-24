"""Declare runtime dependencies
"""

load("@bazel_tools//tools/build_defs/repo:http.bzl", "http_archive")
load("@bazel_tools//tools/build_defs/repo:utils.bzl", "maybe")

def _http_archive(name, **kwargs):
    maybe(http_archive, name = name, **kwargs)

def rules_android_lint_dependencies():
    _http_archive(
        name = "bazel_skylib",
        sha256 = "66ffd9315665bfaafc96b52278f57c7e2dd09f5ede279ea6d39b2be471e7e3aa",
        url = "https://github.com/bazelbuild/bazel-skylib/releases/download/1.4.2/bazel-skylib-1.4.2.tar.gz",
    )

    _http_archive(
        name = "rules_java",
        url = "https://github.com/bazelbuild/rules_java/releases/download/7.0.6/rules_java-7.0.6.tar.gz",
        sha256 = "e81e9deaae0d9d99ef3dd5f6c1b32338447fe16d5564155531ea4eb7ef38854b",
    )

    _http_archive(
        name = "rules_kotlin",
        sha256 = "5766f1e599acf551aa56f49dab9ab9108269b03c557496c54acaf41f98e2b8d6",
        url = "https://github.com/buildfoundation/rules_kotlin/releases/download/v1.9.0/rules_kotlin-v1.9.0.tar.gz",
    )

    _http_archive(
        name = "platforms",
        url = "https://github.com/bazelbuild/platforms/releases/download/0.0.8/platforms-0.0.8.tar.gz",
        sha256 = "8150406605389ececb6da07cbcb509d5637a3ab9a24bc69b1101531367d89d74",
    )

    _http_archive(
        name = "buildifier_prebuilt",
        sha256 = "72b5bb0853aac597cce6482ee6c62513318e7f2c0050bc7c319d75d03d8a3875",
        strip_prefix = "buildifier-prebuilt-6.3.3",
        url = "http://github.com/keith/buildifier-prebuilt/archive/6.3.3.tar.gz",
    )

    _http_archive(
        name = "aspect_bazel_lib",
        sha256 = "e3151d87910f69cf1fc88755392d7c878034a69d6499b287bcfc00b1cf9bb415",
        strip_prefix = "bazel-lib-1.32.1",
        url = "https://github.com/aspect-build/bazel-lib/releases/download/v1.32.1/bazel-lib-v1.32.1.tar.gz",
    )

    _http_archive(
        name = "rules_jvm_external",
        strip_prefix = "rules_jvm_external-5.3",
        sha256 = "d31e369b854322ca5098ea12c69d7175ded971435e55c18dd9dd5f29cc5249ac",
        url = "https://github.com/bazelbuild/rules_jvm_external/releases/download/5.3/rules_jvm_external-5.3.tar.gz",
    )

# buildifier: disable=unnamed-macro
def android_lint_register_toolchains(register = True):
    """Convenience macro for users which does typical setup.

    Args:
        register: Whether to register the toolchain. If False, the user must
            register it themselves.
    """
    if register:
        native.register_toolchains("//toolchains:android_lint_default_toolchain")
