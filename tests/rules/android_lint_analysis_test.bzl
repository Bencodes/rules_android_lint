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

def _argument_values(argv, flag):
    return [
        argv[index + 1]
        for index in range(len(argv) - 1)
        if argv[index] == flag
    ]

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
        module_name = _argument_value(argv, "--label")
        output = _argument_value(argv, "--output")
        dependency_partial_results = _argument_values(argv, "--dependency-partial-results")
        android_dependencies = _argument_values(argv, "--android-dependency")
        library_dependencies = _argument_values(argv, "--library-dependency")
        classpath_aars = _argument_values(argv, "--classpath-aar")

        asserts.true(env, module_name != None and module_name.endswith("%3Aanalysis_fixture_lint"))
        asserts.false(env, "/" in module_name)
        asserts.false(env, ":" in module_name)
        asserts.false(env, "=" in module_name)
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

        dependency_module_names = [value.split("=")[0] for value in dependency_partial_results]
        asserts.equals(env, 5, len(dependency_partial_results))
        asserts.true(
            env,
            any([name.endswith("%3Aandroid_dependency") for name in dependency_module_names]),
        )
        asserts.true(
            env,
            any([name.endswith("%3Acollision-dep") for name in dependency_module_names]),
        )
        asserts.true(
            env,
            any([name.endswith("%3Acollision.dep") for name in dependency_module_names]),
        )
        asserts.true(
            env,
            any([name.endswith("%3Aruntime_android_dependency") for name in dependency_module_names]),
        )
        asserts.true(
            env,
            any([name.endswith("%3Aruntime_parent") for name in dependency_module_names]),
        )
        asserts.equals(env, 2, len(android_dependencies))
        asserts.true(
            env,
            any([name.endswith("%3Aandroid_dependency") for name in android_dependencies]),
        )
        asserts.true(
            env,
            any([name.endswith("%3Aruntime_android_dependency") for name in android_dependencies]),
        )
        for android_dependency in android_dependencies:
            asserts.true(env, android_dependency in dependency_module_names)
        asserts.equals(env, 5, len(library_dependencies))
        for library_dependency in library_dependencies:
            asserts.true(env, library_dependency in dependency_module_names)
        asserts.true(
            env,
            any(["runtime_android_dependency.aar:" in aar for aar in classpath_aars]),
        )

        action_inputs = action.inputs.to_list()
        inputs = [file.basename for file in action_inputs]
        outputs = [file.basename for file in action.outputs.to_list()]
        asserts.true(env, "Fixture.java" in inputs)
        asserts.true(
            env,
            any(["compose-lint-checks" in input and input.endswith(".jar") for input in inputs]),
        )
        asserts.true(env, "analysis_fixture_lint.xml" in outputs)
        for value in dependency_partial_results:
            partial_results_path = value.split("=", 1)[1]
            asserts.true(env, partial_results_path in [file.path for file in action_inputs])

        asserts.equals(env, "1", action.execution_info.get("supports-workers"))
        asserts.equals(env, "1", action.execution_info.get("supports-multiplex-workers"))

    return analysistest.end(env)

_android_lint_action_test = analysistest.make(
    _android_lint_action_impl,
    config_settings = {
        "//command_line_option:extra_toolchains": [
            "//tests/rules:dependency_analysis_enabled_toolchain",
        ],
    },
)

def _android_dependency_analysis_impl(ctx):
    env = analysistest.begin(ctx)
    target = analysistest.target_under_test(env)
    asserts.true(env, _AndroidDependencyAnalysisInfo in target)
    if _AndroidDependencyAnalysisInfo in target:
        analysis = target[_AndroidDependencyAnalysisInfo]
        info = analysis.lint_info
        asserts.true(env, info.is_android)
        asserts.true(env, info.is_library)
        asserts.true(env, info.partial_results != None)
        asserts.equals(env, "AndroidManifest.xml", info.manifest.basename)
        asserts.true(
            env,
            "strings.xml" in [file.basename for file in info.resource_files.to_list()],
        )

        actions = [action for action in analysis.actions if action.mnemonic == "AndroidLintAnalyze"]
        asserts.equals(env, 1, len(actions))
        if actions:
            action = actions[0]
            argv = action.argv
            resource = _argument_value(argv, "--resource")
            manifest = _argument_value(argv, "--android-manifest")
            android_home = _argument_value(argv, "--android-home")
            module_name = _argument_value(argv, "--label")
            classpath_aars = _argument_values(argv, "--classpath-aar")

            asserts.true(env, "--android" in argv)
            asserts.true(env, "--library" in argv)
            asserts.equals(env, 0, len(classpath_aars))
            asserts.true(env, module_name != None and module_name.endswith("%3Aandroid_dependency"))
            asserts.true(env, resource != None and resource.endswith("/res/values/strings.xml"))
            asserts.true(env, manifest != None and manifest.endswith("/AndroidManifest.xml"))
            asserts.true(env, android_home != None and "androidsdk" in android_home)

            inputs = [file.path for file in action.inputs.to_list()]
            asserts.true(env, any([path.endswith("/res/values/strings.xml") for path in inputs]))
            asserts.true(env, any([path.endswith("/AndroidManifest.xml") for path in inputs]))
            asserts.true(
                env,
                any([
                    "/platforms/android-" in path and path.endswith("/android.jar")
                    for path in inputs
                ]),
            )

    return analysistest.end(env)

_android_dependency_analysis_test = analysistest.make(_android_dependency_analysis_impl)

_AndroidDependencyAnalysisInfo = provider(
    "Test-only view of the dependency lint aspect's provider and registered actions.",
    fields = ["actions", "lint_info"],
)

def _android_dependency_analysis_aspect_impl(target, _ctx):
    return [_AndroidDependencyAnalysisInfo(
        actions = target.actions,
        lint_info = target[AndroidLintPartialResultsInfo],
    )]

_android_dependency_analysis_aspect = aspect(
    implementation = _android_dependency_analysis_aspect_impl,
    requires = [lint_analysis_aspect],
)

def _android_dependency_subject_impl(ctx):
    return [ctx.attr.dep[_AndroidDependencyAnalysisInfo]]

_android_dependency_subject = rule(
    implementation = _android_dependency_subject_impl,
    attrs = {
        "dep": attr.label(
            aspects = [_android_dependency_analysis_aspect],
            providers = [_AndroidDependencyAnalysisInfo],
        ),
    },
)

_ModuleNamesInfo = provider(
    "Test-only collection of module IDs propagated by dependency lint aspects.",
    fields = ["module_names"],
)

def _module_names_subject_impl(ctx):
    module_names = []
    for dep in ctx.attr.deps:
        module_names.extend([
            node.module_name
            for node in dep[AndroidLintPartialResultsInfo].transitive_results.to_list()
        ])
    return [_ModuleNamesInfo(module_names = module_names)]

_module_names_subject = rule(
    implementation = _module_names_subject_impl,
    attrs = {
        "deps": attr.label_list(
            aspects = [lint_analysis_aspect],
            providers = [AndroidLintPartialResultsInfo],
        ),
    },
)

def _module_name_collision_impl(ctx):
    env = analysistest.begin(ctx)
    target = analysistest.target_under_test(env)
    module_names = target[_ModuleNamesInfo].module_names

    asserts.equals(env, 2, len(module_names))
    if len(module_names) == 2:
        asserts.false(env, module_names[0] == module_names[1])
        asserts.true(env, any([name.endswith("%3Acollision-dep") for name in module_names]))
        asserts.true(env, any([name.endswith("%3Acollision.dep") for name in module_names]))
        for module_name in module_names:
            asserts.false(env, "/" in module_name)
            asserts.false(env, ":" in module_name)
            asserts.false(env, "=" in module_name)

    return analysistest.end(env)

_module_name_collision_test = analysistest.make(_module_name_collision_impl)

def _dependency_analysis_enabled_transition_impl(_settings, _attr):
    return {
        "//command_line_option:extra_toolchains": [
            "//tests/rules:dependency_analysis_enabled_toolchain",
        ],
    }

_dependency_analysis_enabled_transition = transition(
    implementation = _dependency_analysis_enabled_transition_impl,
    inputs = [],
    outputs = ["//command_line_option:extra_toolchains"],
)

def _dependency_analysis_enabled_lint_impl(ctx):
    # An attribute transition exposes a singleton list even for attr.label.
    info = ctx.attr.lint[0][AndroidLintResultsInfo]
    return [
        info,
        DefaultInfo(files = depset([info.output])),
    ]

dependency_analysis_enabled_lint = rule(
    doc = "Forwards a lint report built with dependency analysis enabled.",
    implementation = _dependency_analysis_enabled_lint_impl,
    attrs = {
        "lint": attr.label(
            cfg = _dependency_analysis_enabled_transition,
            mandatory = True,
            providers = [AndroidLintResultsInfo],
        ),
        "_allowlist_function_transition": attr.label(
            default = "@bazel_tools//tools/allowlists/function_transition_allowlist",
        ),
    },
)

def android_lint_analysis_test_suite(name, additional_tests = []):
    """Defines the android_lint analysis test suite.

    Args:
      name: Name of the generated test suite.
      additional_tests: Additional test labels to include in the suite.
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
    module_names_subject = name + "_module_names_subject"
    _module_names_subject(
        name = module_names_subject,
        deps = [
            ":collision-dep",
            ":collision.dep",
        ],
    )
    module_name_collision_test = name + "_module_name_collision_test"
    _module_name_collision_test(
        name = module_name_collision_test,
        target_under_test = ":" + module_names_subject,
    )
    native.test_suite(
        name = name,
        tests = [
            ":" + action_test,
            ":" + android_dependency_test,
            ":" + module_name_collision_test,
        ] + additional_tests,
    )
