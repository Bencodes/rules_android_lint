"""Android lint rule."""

load(
    ":impl.bzl",
    _process_android_lint_issues = "process_android_lint_issues",
)
load(
    ":attrs.bzl",
    _ATTRS = "ATTRS",
)
load(
    ":providers.bzl",
    _AndroidLintResultsInfo = "AndroidLintResultsInfo",
)
load(
    "@bazel_skylib//lib:dicts.bzl",
    _dicts = "dicts",
)

def _impl(ctx):
    android_lint_results = _process_android_lint_issues(ctx, regenerate = False)

    inputs = []
    inputs.append(android_lint_results.xml_output)

    files_output = []
    if android_lint_results.xml_output != None:
        files_output.append(android_lint_results.xml_output)
    if android_lint_results.html_output != None:
        files_output.append(android_lint_results.html_output)
    return [
        DefaultInfo(
            runfiles = ctx.runfiles(files = inputs),
            files = depset(files_output),
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
