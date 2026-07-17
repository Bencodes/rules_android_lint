"""Analysis tests for android_lint action and provider wiring."""

load("@bazel_skylib//lib:unittest.bzl", "analysistest", "asserts")
load("//rules:lint_analysis_aspect.bzl", "lint_analysis_aspect")
load(
    "//rules:providers.bzl",
    "AndroidLintPartialResultsInfo",
    "AndroidLintResultsInfo",
)

def _argument_value(argv, flag):
    for index in range(len(argv)):
        if argv[index] == flag:
            if index + 1 < len(argv):
                return argv[index + 1]
            return None
    return None

def _android_lint_action_impl(ctx):
    env = analysistest.begin(ctx)
    target = analysistest.target_under_test(env)
    asserts.true(env, AndroidLintResultsInfo in target)
    asserts.equals(env, "analysis_fixture_lint.xml", target[AndroidLintResultsInfo].output.basename)

    actions = [
        action
        for action in analysistest.target_actions(env)
        if action.mnemonic == "AndroidLint"
    ]
    asserts.equals(env, 1, len(actions))
    if actions:
        action = actions[0]
        argv = action.argv
        source = _argument_value(argv, "--src")
        custom_rule = _argument_value(argv, "--custom-rule")
        output = _argument_value(argv, "--output")

        asserts.true(env, source != None and source.endswith("/Fixture.java"))
        asserts.true(
            env,
            custom_rule != None and "compose-lint-checks" in custom_rule and custom_rule.endswith(".jar"),
        )
        asserts.equals(env, "DefaultLocale", _argument_value(argv, "--disable-check"))
        asserts.true(env, "--warnings-as-errors" in argv)
        asserts.equals(env, target[AndroidLintResultsInfo].output.path, output)
        asserts.false(env, "--baseline-file" in argv)
        asserts.false(env, "--regenerate-baseline-files" in argv)

        inputs = [file.basename for file in action.inputs.to_list()]
        outputs = [file.basename for file in action.outputs.to_list()]
        asserts.true(env, "Fixture.java" in inputs)
        asserts.true(
            env,
            any(["compose-lint-checks" in input and input.endswith(".jar") for input in inputs]),
        )
        asserts.true(env, "analysis_fixture_lint.xml" in outputs)

        asserts.equals(env, "1", action.execution_info.get("supports-workers"))
        asserts.equals(env, "1", action.execution_info.get("supports-multiplex-workers"))

    return analysistest.end(env)

_android_lint_action_test = analysistest.make(_android_lint_action_impl)

def _android_dependency_analysis_impl(ctx):
    env = analysistest.begin(ctx)
    target = analysistest.target_under_test(env)
    asserts.true(env, AndroidLintPartialResultsInfo in target)
    if AndroidLintPartialResultsInfo in target:
        info = target[AndroidLintPartialResultsInfo]
        asserts.true(env, info.is_android)
        asserts.true(env, info.partial_results != None)
        asserts.equals(env, "AndroidManifest.xml", info.manifest.basename)
        asserts.true(
            env,
            "strings.xml" in [file.basename for file in info.resource_files.to_list()],
        )

    return analysistest.end(env)

_android_dependency_analysis_test = analysistest.make(_android_dependency_analysis_impl)

def _android_dependency_subject_impl(ctx):
    info = ctx.attr.dep[AndroidLintPartialResultsInfo]
    return [
        DefaultInfo(files = depset([info.partial_results])),
        info,
    ]

_android_dependency_subject = rule(
    implementation = _android_dependency_subject_impl,
    attrs = {
        "dep": attr.label(
            aspects = [lint_analysis_aspect],
            providers = [AndroidLintPartialResultsInfo],
        ),
    },
)

def android_lint_analysis_test_suite(name):
    """Defines the android_lint analysis test suite.

    Args:
      name: Name of the generated test suite.
    """
    action_test = name + "_action_test"
    _android_lint_action_test(
        name = action_test,
        target_under_test = ":analysis_fixture_lint",
    )
    android_dependency_subject = name + "_android_dependency_subject"
    _android_dependency_subject(
        name = android_dependency_subject,
        dep = ":android_dependency",
    )
    android_dependency_test = name + "_android_dependency_test"
    _android_dependency_analysis_test(
        name = android_dependency_test,
        target_under_test = ":" + android_dependency_subject,
    )
    native.test_suite(
        name = name,
        tests = [
            ":" + action_test,
            ":" + android_dependency_test,
        ],
    )
