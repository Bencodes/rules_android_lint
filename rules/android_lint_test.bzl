"""Android lint test rule."""

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

def _test_impl(ctx):
    android_lint_results = _process_android_lint_issues(ctx, regenerate = False)

    inputs = []
    inputs.append(android_lint_results.output)
    inputs.extend(ctx.attr._android_lint_output_validator.default_runfiles.files.to_list())

    ctx.actions.write(
        output = ctx.outputs.executable,
        is_executable = False,
        content = """
#!/bin/bash

{executable} --lint_baseline "{lint_baseline}"
        """.format(
            executable = ctx.executable._android_lint_output_validator.short_path,
            lint_baseline = android_lint_results.output.short_path,
        ),
    )

    return [
        DefaultInfo(
            runfiles = ctx.runfiles(files = inputs),
            executable = ctx.outputs.executable,
            files = depset([ctx.outputs.executable, android_lint_results.output]),
        ),
    ] + android_lint_results.providers

android_lint_test = rule(
    implementation = _test_impl,
    attrs = _dicts.add(
        _ATTRS,
        dict(
            baseline = attr.label(
                mandatory = False,
                allow_single_file = True,
                doc = "Lint baseline file.",
            ),
            _android_lint_output_validator = attr.label(
                default = Label("//src:android_lint_output_validator"),
                executable = True,
                cfg = "exec",
            ),
        ),
    ),
    provides = [
        _AndroidLintResultsInfo,
    ],
    test = True,
    toolchains = [
        "//toolchains:toolchain_type",
    ],
)
