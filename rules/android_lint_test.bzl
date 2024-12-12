"""Android lint test rule."""

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

def _test_impl(ctx):
    android_lint_results = _process_android_lint_issues(ctx, regenerate = False)

    inputs = []
    inputs.append(android_lint_results.xml_output)
    inputs.extend(ctx.attr._android_lint_output_validator.default_runfiles.files.to_list())

    ctx.actions.write(
        output = ctx.outputs.executable,
        is_executable = False,
        content = """
#!/bin/bash

{executable} --lint_baseline "{lint_baseline}"
        """.format(
            executable = ctx.executable._android_lint_output_validator.short_path,
            lint_baseline = android_lint_results.xml_output.short_path,
        ),
    )
    files_info = [ctx.outputs.executable]
    if android_lint_results.xml_output != None:
        files_info.append(android_lint_results.xml_output)
    if android_lint_results.html_output != None:
        files_info.append(android_lint_results.html_output)
    return [
        DefaultInfo(
            runfiles = ctx.runfiles(files = inputs),
            executable = ctx.outputs.executable,
            files = depset(files_info),
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
