"""Rule implementation for Android Lint
"""

load("@rules_java//java/common:java_info.bzl", "JavaInfo")
load(
    ":collect_aar_outputs_aspect.bzl",
    _AndroidLintAARInfo = "AndroidLintAARInfo",
)
load(
    ":providers.bzl",
    _AndroidLintResultsInfo = "AndroidLintResultsInfo",
)
load(
    ":utils.bzl",
    _ANDROID_LINT_TOOLCHAIN_TYPE = "ANDROID_LINT_TOOLCHAIN_TYPE",
    _utils = "utils",
)

def _run_android_lint(
        ctx,
        android_lint,
        module_name,
        output,
        srcs,
        deps,
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
        android_lint_enable_check_dependencies,
        android_lint_skip_bytecode_verifier):
    """Constructs the Android Lint actions

    Args:
        ctx: The target context
        android_lint: The Android Lint binary to use
        module_name: The name of the module
        output: The output file
        srcs: The source files
        deps: Depset of aars and jars to include on the classpath
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
        android_lint_enable_check_dependencies: Enables dependency checking during analysis
        android_lint_skip_bytecode_verifier: Disables bytecode verification
    """
    inputs = []
    outputs = [output]

    args = ctx.actions.args()
    args.set_param_file_format("multiline")
    args.use_param_file("@%s", use_always = True)

    args.add("--android-lint-cli-tool", android_lint)
    inputs.append(android_lint)
    args.add("--label", "{}".format(module_name))
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
    if not regenerate and baseline:
        args.add("--baseline-file", baseline)
        inputs.append(baseline)
    if regenerate:
        args.add("--regenerate-baseline-files")
    if config:
        args.add("--config-file", config)
        inputs.append(config)
    if warnings_as_errors:
        args.add("--warnings-as-errors")
    for custom_rule in _utils.list_or_depset_to_list(custom_rules):
        args.add("--custom-rule", custom_rule)
        inputs.append(custom_rule)
    if autofix == True:
        args.add("--autofix")
    for check in disable_checks:
        args.add("--disable-check", check)
    for check in enable_checks:
        args.add("--enable-check", check)
    for dep in _utils.list_or_depset_to_list(deps):
        if not dep.path.endswith(".aar") and not dep.path.endswith(".jar"):
            continue
        args.add("--classpath", dep)
        inputs.append(dep)
    if android_lint_enable_check_dependencies:
        args.add("--enable-check-dependencies")

    # Declare the output file
    args.add("--output", output)
    outputs.append(output)

    toolchain = _utils.get_android_lint_toolchain(ctx)
    if toolchain.android_home != None:
        args.add("--android-home", toolchain.android_home.label.workspace_root)

    ctx.actions.run(
        mnemonic = "AndroidLint",
        inputs = inputs,
        outputs = outputs,
        executable = ctx.executable._lint_wrapper,
        progress_message = "Running Android Lint {}".format(str(ctx.label)),
        arguments = [args],
        tools = [ctx.executable._lint_wrapper],
        toolchain = _ANDROID_LINT_TOOLCHAIN_TYPE,
        execution_requirements = {
            "supports-workers": "1",
            "supports-multiplex-workers": "1",
            "requires-worker-protocol": "json",
        },
        env = {
            # https://googlesamples.github.io/android-custom-lint-rules/usage/variables.md.html
            "ANDROID_LINT_SKIP_BYTECODE_VERIFIER": ("true" if android_lint_skip_bytecode_verifier else "false"),
        },
    )

def _get_module_name(ctx):
    """Extracts the module name from the target

    This module name will be embedded in the Android Lint project configuration.

    Args:
        ctx: The target context

    Returns:
        A string representing the module name
    """
    path = ctx.build_file_path.split("BUILD")[0].replace("/", "_").replace("-", "_").replace(".", "_")
    name = ctx.attr.name
    if path:
        return "%s_%s" % (path.replace("/", "_").replace("-", "_"), ctx.attr.name)
    return name

def process_android_lint_issues(ctx, regenerate):
    """Runs Android Lint for the given target

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
        manifest = ctx.actions.declare_file("AndroidManifest.xml")
        ctx.actions.symlink(output = manifest, target_file = ctx.file.manifest)

    # Collect the transitive classpath jars to run lint against.
    deps = []
    for dep in ctx.attr.deps:
        if JavaInfo in dep:
            deps.append(dep[JavaInfo].compile_jars)
        if AndroidLibraryResourceClassJarProvider in dep:
            deps.append(dep[AndroidLibraryResourceClassJarProvider].jars)
        if AndroidLibraryAarInfo in dep:
            deps.append(dep[AndroidLibraryAarInfo].transitive_aar_artifacts)
        if _AndroidLintAARInfo in dep:
            deps.append(dep[_AndroidLintAARInfo].aars)

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

    output = ctx.actions.declare_file("{}.xml".format(ctx.label.name))
    _run_android_lint(
        ctx,
        android_lint = _utils.only(_utils.list_or_depset_to_list(_utils.get_android_lint_toolchain(ctx).android_lint.files)),
        module_name = _get_module_name(ctx),
        output = output,
        srcs = ctx.files.srcs,
        deps = depset(transitive = deps),
        resource_files = ctx.files.resource_files,
        manifest = manifest,
        compile_sdk_version = _utils.get_android_lint_toolchain(ctx).compile_sdk_version,
        java_language_level = _utils.get_android_lint_toolchain(ctx).java_language_level,
        kotlin_language_level = _utils.get_android_lint_toolchain(ctx).kotlin_language_level,
        baseline = getattr(ctx.file, "baseline", None),
        config = config,
        warnings_as_errors = ctx.attr.warnings_as_errors,
        custom_rules = ctx.files.custom_rules,
        disable_checks = ctx.attr.disable_checks,
        enable_checks = ctx.attr.enable_checks,
        autofix = ctx.attr.autofix,
        regenerate = regenerate,
        android_lint_enable_check_dependencies = _utils.get_android_lint_toolchain(ctx).android_lint_enable_check_dependencies,
        android_lint_skip_bytecode_verifier = _utils.get_android_lint_toolchain(ctx).android_lint_skip_bytecode_verifier,
    )

    return struct(
        output = output,
        providers = [
            _AndroidLintResultsInfo(
                output = output,
            ),
        ],
    )
