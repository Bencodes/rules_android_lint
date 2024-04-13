"""Declare runtime dependencies
"""

load("@bazel_tools//tools/build_defs/repo:http.bzl", _http_archive = "http_archive")
load("@bazel_tools//tools/build_defs/repo:utils.bzl", "maybe")

def _http_archive(name, **kwargs):
    maybe(_http_archive, name = name, **kwargs)

def rules_android_lint_dependencies():
    # The minimal version of bazel_skylib we require
    _http_archive(
        name = "bazel_skylib",
        sha256 = "74d544d96f4a5bb630d465ca8bbcfe231e3594e5aae57e1edbf17a6eb3ca2506",
        urls = [
            "https://github.com/bazelbuild/bazel-skylib/releases/download/1.5.0/bazel-skylib-1.3.0.tar.gz",
            "https://mirror.bazel.build/github.com/bazelbuild/bazel-skylib/releases/download/1.3.0/bazel-skylib-1.3.0.tar.gz",
        ],
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
