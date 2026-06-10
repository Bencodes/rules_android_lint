"""Aspect that runs the Android Lint analysis (`--analyze-only`) phase per target.

For each first-party target it visits, the aspect runs lint in analyze mode, producing a
per-target partial-results directory, and propagates those outputs up the dependency graph via
[AndroidLintPartialResultsInfo]. A leaf rule later runs the report (`--report-only`) phase over
the transitive set. The analysis of a target is isolated: it depends only on that target's own
sources plus its dependencies' compiled artifacts, never on another target's partial results.
"""

load(
    "@rules_android//providers:providers.bzl",
    "AndroidLibraryResourceClassJarProvider",
)
load("@rules_java//java:defs.bzl", "JavaInfo", "java_common")
load(
    ":collect_aar_outputs_aspect.bzl",
    _AndroidLintAARInfo = "AndroidLintAARInfo",
)
load(
    ":providers.bzl",
    _AndroidLintPartialResultsInfo = "AndroidLintPartialResultsInfo",
)
load(
    ":utils.bzl",
    _ANDROID_LINT_TOOLCHAIN_TYPE = "ANDROID_LINT_TOOLCHAIN_TYPE",
    _utils = "utils",
)

# Edges traversed by the analysis aspect, matching collect_aar_outputs_aspect.
_ANALYSIS_ATTR_ASPECTS = ["deps", "exports", "associates"]

def _module_name(label):
    """Derives a stable, unique lint module name from a target label."""
    return ("%s_%s" % (label.package, label.name)).replace("/", "_").replace("-", "_").replace(".", "_").replace(":", "_")

def _aspect_deps(ctx):
    deps = []
    for attr in _ANALYSIS_ATTR_ASPECTS:
        deps.extend(getattr(ctx.rule.attr, attr, []))
    return deps

def _collect_transitive(deps):
    return [
        dep[_AndroidLintPartialResultsInfo].transitive_results
        for dep in deps
        if _AndroidLintPartialResultsInfo in dep
    ]

def _lint_analysis_aspect_impl(target, ctx):
    deps = _aspect_deps(ctx)
    transitive = _collect_transitive(deps)

    # Skip nodes that have nothing to analyze (no JavaInfo or no sources), but keep propagating
    # the transitive results gathered from their dependencies.
    srcs = getattr(ctx.rule.files, "srcs", [])
    if JavaInfo not in target or not srcs:
        return [_AndroidLintPartialResultsInfo(
            partial_results = None,
            module_name = None,
            transitive_results = depset(transitive = transitive),
        )]

    toolchain = _utils.get_android_lint_toolchain(ctx)
    module_name = _module_name(ctx.label)
    partial_results = ctx.actions.declare_directory("_lint/%s/partial_results" % ctx.label.name)
    android_lint = _utils.only(_utils.list_or_depset_to_list(toolchain.android_lint.files))
    java_runtime_info = ctx.attr._javabase[java_common.JavaRuntimeInfo]

    inputs = [android_lint]
    inputs.extend(srcs)

    args = ctx.actions.args()
    args.set_param_file_format("multiline")
    args.use_param_file("@%s", use_always = True)

    args.add("--android-lint-cli-tool", android_lint)
    args.add("--label", module_name)
    args.add("--mode", "analyze")
    args.add("--partial-results", partial_results.path)
    if toolchain.compile_sdk_version:
        args.add("--compile-sdk-version", toolchain.compile_sdk_version)
    if toolchain.java_language_level:
        args.add("--java-language-level", toolchain.java_language_level)
    if toolchain.kotlin_language_level:
        args.add("--kotlin-language-level", toolchain.kotlin_language_level)

    for src in srcs:
        args.add("--src", src)

    # Classpath for symbol resolution: this target's full compile classpath (its own outputs plus
    # its transitive dependencies) and any compiled R classes.
    classpath = [target[JavaInfo].transitive_compile_time_jars]
    if AndroidLibraryResourceClassJarProvider in target:
        classpath.append(target[AndroidLibraryResourceClassJarProvider].jars)
    for jar in depset(transitive = classpath).to_list():
        args.add("--classpath-jar", jar)
        inputs.append(jar)

    # AAR dependencies (extracted), so lint loads their classes and embedded lint.jar checks.
    if _AndroidLintAARInfo in target:
        for node in target[_AndroidLintAARInfo].transitive_aar_artifacts.to_list():
            if node.aar and node.aar_dir:
                args.add("--classpath-aar", "%s:%s" % (node.aar.path, node.aar_dir.path))
                inputs.append(node.aar)
                inputs.append(node.aar_dir)

    if toolchain.android_lint_config:
        config = _utils.only(_utils.list_or_depset_to_list(toolchain.android_lint_config.files))
        args.add("--config-file", config)
        inputs.append(config)

    if toolchain.android_home != None:
        args.add("--android-home", toolchain.android_home.label.workspace_root)

    if java_runtime_info:
        args.add("--jdk-home", java_runtime_info.java_home)
        inputs.extend(java_runtime_info.files.to_list())

    ctx.actions.run(
        mnemonic = "AndroidLintAnalyze",
        inputs = inputs,
        outputs = [partial_results],
        executable = ctx.executable._lint_wrapper,
        progress_message = "Analyzing Android Lint {}".format(str(ctx.label)),
        arguments = [args],
        tools = [ctx.executable._lint_wrapper],
        toolchain = _ANDROID_LINT_TOOLCHAIN_TYPE,
        execution_requirements = {
            "supports-workers": "1",
            "supports-multiplex-workers": "1",
        },
        env = {
            "ANDROID_LINT_SKIP_BYTECODE_VERIFIER": (
                "true" if toolchain.android_lint_skip_bytecode_verifier else "false"
            ),
        },
    )

    direct = struct(module_name = module_name, partial_results = partial_results)
    return [_AndroidLintPartialResultsInfo(
        partial_results = partial_results,
        module_name = module_name,
        transitive_results = depset(direct = [direct], transitive = transitive),
    )]

lint_analysis_aspect = aspect(
    implementation = _lint_analysis_aspect_impl,
    attr_aspects = _ANALYSIS_ATTR_ASPECTS,
    attrs = {
        "_lint_wrapper": attr.label(
            default = Label("//src/cli"),
            executable = True,
            cfg = "exec",
        ),
        "_javabase": attr.label(
            default = "@bazel_tools//tools/jdk:current_java_runtime",
        ),
    },
    provides = [_AndroidLintPartialResultsInfo],
    required_aspect_providers = [[_AndroidLintAARInfo]],
    toolchains = ["//toolchains:toolchain_type"],
)
