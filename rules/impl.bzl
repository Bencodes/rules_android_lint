"""Rule implementation for Android Lint
"""

load(
    "@rules_android//providers:providers.bzl",
    "AndroidLibraryResourceClassJarProvider",
    "StarlarkAndroidResourcesInfo",
)
load("@rules_java//java:defs.bzl", "JavaInfo", "java_common")
load(
    ":collect_aar_outputs_aspect.bzl",
    _AndroidLintAARInfo = "AndroidLintAARInfo",
)
load(
    ":providers.bzl",
    _AndroidLintPartialResultsInfo = "AndroidLintPartialResultsInfo",
    _AndroidLintResultsInfo = "AndroidLintResultsInfo",
)
load(
    ":utils.bzl",
    _ANDROID_LINT_TOOLCHAIN_TYPE = "ANDROID_LINT_TOOLCHAIN_TYPE",
    _utils = "utils",
)

def _run_android_lint(
        ctx,
        mode,
        android_lint,
        is_android,
        module_name,
        output,
        partial_results,
        dependency_modules,
        srcs,
        deps,
        aars,
        resource_files,
        manifest,
        compile_sdk_version,
        java_language_level,
        kotlin_language_level,
        baseline,
        config,
        warnings_as_errors,
        custom_rules,
        disable_checks,
        enable_checks,
        autofix,
        regenerate,
        android_lint_skip_bytecode_verifier,
        android_lint_toolchain,
        java_runtime_info):
    """Constructs an Android Lint action for the given phase.

    Args:
        ctx: The target context
        mode: One of "analyze" (write partial results) or "report" (consume them, emit XML)
        android_lint: The Android Lint binary to use
        is_android: Whether the module is an Android module
        module_name: The name of the module
        output: The XML output file (report mode only; None in analyze mode)
        partial_results: The partial-results directory (output in analyze, input in report)
        dependency_modules: List of structs(module_name, partial_results) for first-party deps
            whose partial results should be merged (report mode only)
        srcs: The source files
        deps: Depset of aars and jars to include on the classpath
        aars: Depset of the aar nodes
        resource_files: The Android resource files
        manifest: The Android manifest file
        compile_sdk_version: The Android compile SDK version
        java_language_level: The Java language level
        kotlin_language_level: The Kotlin language level
        baseline: The Android Lint baseline file
        config: The Android Lint config file
        warnings_as_errors: Whether to treat warnings as errors
        custom_rules: List of jars containing the custom rules
        disable_checks: List of additional checks to disable
        enable_checks: List of additional checks to enable
        autofix: Whether to autofix (This is a no-op feature right now)
        regenerate: Whether to regenerate the baseline files
        android_lint_skip_bytecode_verifier: Disables bytecode verification
        android_lint_toolchain: The android lint toolchain
        java_runtime_info: The java runtime toolchain info
    """
    is_report = mode == "report"
    inputs = []
    outputs = []

    args = ctx.actions.args()
    args.set_param_file_format("multiline")
    args.use_param_file("@%s", use_always = True)

    args.add("--android-lint-cli-tool", android_lint)
    inputs.append(android_lint)
    args.add("--label", module_name)
    args.add("--mode", mode)
    if is_android:
        args.add("--android")

    # The partial-results directory is an output of analyze and an input of report.
    args.add("--partial-results", partial_results.path)
    if is_report:
        inputs.append(partial_results)
    else:
        outputs.append(partial_results)

    if compile_sdk_version:
        args.add("--compile-sdk-version", compile_sdk_version)
    if java_language_level:
        args.add("--java-language-level", java_language_level)
    if kotlin_language_level:
        args.add("--kotlin-language-level", kotlin_language_level)
    for src in _utils.list_or_depset_to_list(srcs):
        args.add("--src", src)
        inputs.append(src)
    for resource_file in _utils.list_or_depset_to_list(resource_files):
        args.add("--resource", resource_file)
        inputs.append(resource_file)
    if manifest:
        args.add("--android-manifest", manifest)
        inputs.append(manifest)
    if config:
        args.add("--config-file", config)
        inputs.append(config)
    for custom_rule in _utils.list_or_depset_to_list(custom_rules):
        args.add("--custom-rule", custom_rule)
        inputs.append(custom_rule)
    for check in disable_checks:
        args.add("--disable-check", check)
    for check in enable_checks:
        args.add("--enable-check", check)
    for dep in _utils.list_or_depset_to_list(deps):
        if not dep.path.endswith(".jar"):
            fail("Error: Found artifact that is not an aar: %s" % dep.path)
        args.add("--classpath-jar", dep)
        inputs.append(dep)
    for aar_node_info in _utils.list_or_depset_to_list(aars):
        aar = aar_node_info.aar
        aar_dir = aar_node_info.aar_dir
        if aar and aar_dir:
            args.add("--classpath-aar", "%s:%s" % (aar.path, aar_dir.path))
            inputs.append(aar)
            inputs.append(aar_dir)

    # Report-phase-only arguments: baseline, reporting filters, output, and the dependency
    # partial results merged into the final verdict.
    if is_report:
        if not regenerate and baseline:
            args.add("--baseline-file", baseline)
            inputs.append(baseline)
        if regenerate:
            args.add("--regenerate-baseline-files")
        if warnings_as_errors:
            args.add("--warnings-as-errors")
        if autofix == True:
            args.add("--autofix")
        for dependency in dependency_modules:
            args.add("--dependency-partial-results", "%s=%s" % (dependency.module_name, dependency.partial_results.path))
            if dependency.is_android:
                args.add("--android-dependency", dependency.module_name)
            if dependency.is_library:
                args.add("--library-dependency", dependency.module_name)
            inputs.append(dependency.partial_results)
        args.add("--output", output)
        outputs.append(output)

    if android_lint_toolchain.android_home != None:
        args.add("--android-home", android_lint_toolchain.android_home.label.workspace_root)
        inputs.extend(android_lint_toolchain.android_home.files.to_list())

    if java_runtime_info:
        args.add("--jdk-home", java_runtime_info.java_home)
        inputs.extend(java_runtime_info.files.to_list())

    ctx.actions.run(
        mnemonic = "AndroidLintAnalyze" if mode == "analyze" else "AndroidLint",
        inputs = inputs,
        outputs = outputs,
        executable = ctx.executable._lint_wrapper,
        progress_message = "{} Android Lint {}".format(
            "Analyzing" if mode == "analyze" else "Reporting",
            str(ctx.label),
        ),
        arguments = [args],
        tools = [ctx.executable._lint_wrapper],
        toolchain = _ANDROID_LINT_TOOLCHAIN_TYPE,
        execution_requirements = {
            "supports-workers": "1",
            "supports-multiplex-workers": "1",
        },
        env = {
            # https://googlesamples.github.io/android-custom-lint-rules/usage/variables.md.html
            "ANDROID_LINT_SKIP_BYTECODE_VERIFIER": ("true" if android_lint_skip_bytecode_verifier else "false"),
        },
    )

def _collect_dependency_modules(ctx):
    """Collects the transitive partial-results modules from the rule's dependencies.

    Returns:
        A deduplicated list of structs(module_name, partial_results) for every analyzed
        transitive dependency.
    """
    transitive = []
    for dep in ctx.attr.deps:
        if _AndroidLintPartialResultsInfo in dep:
            transitive.append(dep[_AndroidLintPartialResultsInfo].transitive_results)
    seen = {}
    modules = []
    for node in depset(transitive = transitive).to_list():
        if not node.module_name:
            continue
        previous = seen.get(node.module_name)
        if previous:
            if previous.partial_results.path != node.partial_results.path:
                fail(
                    "Android Lint module ID collision for %s: %s and %s" % (
                        node.module_name,
                        previous.partial_results.path,
                        node.partial_results.path,
                    ),
                )
            continue
        seen[node.module_name] = node
        modules.append(node)
    return modules

def process_android_lint_issues(ctx, regenerate):
    """Runs Android Lint for the given target

    Runs the analysis phase on the target's own sources to produce partial results, then the
    report phase to merge those (and, when check-dependencies is enabled, the dependencies'
    partial results produced by lint_analysis_aspect) into the final XML report.

    Args:
        ctx: The target context
        regenerate: Whether to regenerate the baseline files

    Returns:
        A struct containing the output file and the providers
    """

    # Append the Android manifest file. Lint requires that the input manifest files be named
    # exactly `AndroidManifest.xml`.
    manifest = ctx.file.manifest
    if manifest and manifest.basename != "AndroidManifest.xml":
        manifest = ctx.actions.declare_file("{}/AndroidManifest.xml".format(ctx.label.name))
        ctx.actions.symlink(output = manifest, target_file = ctx.file.manifest)

    # Collect the transitive classpath jars to run lint against.
    deps = []
    aars = []
    for dep in ctx.attr.deps:
        if JavaInfo in dep:
            deps.append(dep[JavaInfo].compile_jars)
        if AndroidLibraryResourceClassJarProvider in dep:
            deps.append(dep[AndroidLibraryResourceClassJarProvider].jars)
        if _AndroidLintAARInfo in dep:
            direct = []
            if dep[_AndroidLintAARInfo].aar:
                direct = [dep[_AndroidLintAARInfo].aar]
            aars.append(depset(
                direct = direct,
                transitive = [
                    dep[_AndroidLintAARInfo].transitive_aar_artifacts,
                ],
            ))

    # Append the compiled R files for our self
    if ctx.attr.lib and AndroidLibraryResourceClassJarProvider in ctx.attr.lib:
        deps.append(ctx.attr.lib[AndroidLibraryResourceClassJarProvider].jars)

    config = None
    if ctx.attr.android_lint_config:
        config = _utils.only(_utils.list_or_depset_to_list(ctx.attr.android_lint_config.files))
    elif _utils.get_android_lint_toolchain(ctx).android_lint_config:
        config = _utils.only(
            _utils.list_or_depset_to_list(_utils.get_android_lint_toolchain(ctx).android_lint_config.files),
        )

    toolchain = _utils.get_android_lint_toolchain(ctx)
    android_lint = _utils.only(_utils.list_or_depset_to_list(toolchain.android_lint.files))
    module_name = _utils.module_name(ctx.label)
    deps_depset = depset(transitive = deps)
    aars_depset = depset(transitive = aars)
    java_runtime_info = ctx.attr._javabase[java_common.JavaRuntimeInfo]

    common = dict(
        android_lint = android_lint,
        is_android = (
            StarlarkAndroidResourcesInfo in ctx.attr.lib or
            manifest != None or
            bool(ctx.files.resource_files)
        ),
        module_name = module_name,
        srcs = ctx.files.srcs,
        deps = deps_depset,
        aars = aars_depset,
        resource_files = ctx.files.resource_files,
        manifest = manifest,
        compile_sdk_version = toolchain.compile_sdk_version,
        java_language_level = toolchain.java_language_level,
        kotlin_language_level = toolchain.kotlin_language_level,
        config = config,
        custom_rules = ctx.files.custom_rules,
        disable_checks = ctx.attr.disable_checks,
        enable_checks = ctx.attr.enable_checks,
        android_lint_skip_bytecode_verifier = toolchain.android_lint_skip_bytecode_verifier,
        android_lint_toolchain = toolchain,
        java_runtime_info = java_runtime_info,
    )

    # Analysis phase: analyze this target's own sources, producing partial results.
    own_partial_results = ctx.actions.declare_directory("{}_lint_partial_results".format(ctx.label.name))
    _run_android_lint(
        ctx,
        mode = "analyze",
        output = None,
        partial_results = own_partial_results,
        dependency_modules = [],
        baseline = None,
        warnings_as_errors = False,
        autofix = False,
        regenerate = False,
        **common
    )

    # Report phase: merge this target's own partial results and, when enabled, the dependencies'.
    dependency_modules = []
    if toolchain.android_lint_enable_check_dependencies:
        dependency_modules = _collect_dependency_modules(ctx)

    output = ctx.actions.declare_file("{}.xml".format(ctx.label.name))
    _run_android_lint(
        ctx,
        mode = "report",
        output = output,
        partial_results = own_partial_results,
        dependency_modules = dependency_modules,
        baseline = getattr(ctx.file, "baseline", None),
        warnings_as_errors = ctx.attr.warnings_as_errors,
        autofix = ctx.attr.autofix,
        regenerate = regenerate,
        **common
    )

    return struct(
        output = output,
        providers = [
            _AndroidLintResultsInfo(
                output = output,
            ),
        ],
    )
