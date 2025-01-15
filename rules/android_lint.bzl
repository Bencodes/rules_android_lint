"""Android lint rule."""

load(
    "@bazel_skylib//lib:dicts.bzl",
    _dicts = "dicts",
)
load(
    ":attrs.bzl",
    _ATTRS = "ATTRS",
)
load(
    ":impl.bzl",
    _process_android_lint_issues = "process_android_lint_issues",
)
load(
    ":providers.bzl",
    _AndroidLintResultsInfo = "AndroidLintResultsInfo",
)

def _impl(ctx):
    android_lint_results = _process_android_lint_issues(ctx, regenerate = False)

    inputs = []
    inputs.append(android_lint_results.output)

    return [
        DefaultInfo(
            runfiles = ctx.runfiles(files = inputs),
            files = depset([android_lint_results.output]),
        ),
    ] + android_lint_results.providers

android_lint = rule(
    implementation = _impl,
    attrs = _dicts.add(
        _ATTRS,
        dict(
        ),
    ),
    provides = [
        _AndroidLintResultsInfo,
    ],
    toolchains = [
        "//toolchains:toolchain_type",
    ],
)
