load("@bazel_skylib//lib:dicts.bzl", "dicts")

AndroidLintResultsInfo = provider(
    "Info needed to evaluate lint results",
    fields = {
        "output": "The Android Lint baseline output",
        "project_configuration": "The Android Lint output configuration file",
    },
)

AndroidLintAARInfo = provider(
    "A provider to collect all aars from transitive dependencies",
    fields = {
        "aars": "depset of aars",
    },
)

def _collect_aar_outputs_aspect(_, ctx):
    deps = getattr(ctx.rule.attr, "deps", [])
    exports = getattr(ctx.rule.attr, "exports", [])
    transitive_aar_depsets = []
    for dep in deps + exports:
        if AndroidLintAARInfo in dep:
            transitive_aar_depsets.append(dep[AndroidLintAARInfo].aars)

    direct_aars = None
    if hasattr(ctx.rule.attr, "aar"):
        aar = ctx.rule.attr.aar.files.to_list()[0]
        direct_aars = [aar]

    return [
        AndroidLintAARInfo(
            aars = depset(
                direct = direct_aars,
                transitive = transitive_aar_depsets,
            ),
        ),
    ]

collect_aar_outputs_aspect = aspect(
    implementation = _collect_aar_outputs_aspect,
    attr_aspects = ["aar", "deps", "exports"],
)

_ATTRS = {
    "_lint_wrapper": attr.label(
        default = "//lint/internal/wrapper:lint_wrapper",
        executable = True,
        cfg = "exec",
        doc = "Implementation for the Lint worker.",
    ),
    "_test_runner_executable": attr.label(
        default = "//lint/internal:test_runner_executable",
        executable = True,
        cfg = "exec",
        doc = "Test runner executible for validating the output results.",
    ),
    "_test_runner_template": attr.label(
        default = "//lint/internal:test_runner_template",
        allow_single_file = True,
    ),
    "srcs": attr.label_list(
        mandatory = True,
        allow_files = [".java", ".kt", ".kts"],
        allow_empty = True,
        doc = "Sources to run Android Lint against.",
    ),
    "resource_files": attr.label_list(
        mandatory = False,
        allow_files = [".xml"],
        allow_empty = True,
        default = [],
        doc = "Android resource files to run Android Lint against.",
    ),
    "self": attr.label(
        mandatory = True,
        doc = "The target being linted.",
    ),
    "manifest": attr.label(
        mandatory = False,
        allow_single_file = True,
        doc = "Android manifest to run Android Lint against.",
    ),
    "deps": attr.label_list(
        mandatory = False,
        allow_empty = True,
        allow_files = True,
        default = [],
        aspects = [collect_aar_outputs_aspect],
        doc = "Dependencies that should be on the classpath during execution.",
    ),
    "config": attr.label(
        mandatory = False,
        allow_single_file = True,
        doc = "Lint configuration file.",
    ),
    "is_test_sources": attr.bool(
        default = False,
        doc = "True when linting test sources, otherwise false.",
    ),
    "autofix": attr.bool(
        default = False,
        doc = "Enables lint autofix",
    ),
    "warnings_as_errors": attr.bool(
        default = False,
        doc = "When true, lint will treat warnings as errors.",
    ),
    "disable_checks": attr.string_list(
        mandatory = False,
        allow_empty = True,
        default = [],
        doc = "List of checks to disable.",
    ),
    "enable_checks": attr.string_list(
        mandatory = False,
        allow_empty = True,
        default = [],
        doc = "List of checks to enable.",
    ),
    "custom_rules": attr.label_list(
        mandatory = False,
        allow_empty = True,
        allow_files = True,
        default = [],
        doc = "Custom lint rules to run.",
    ),
}

def _collect_android_lint_providers(ctx, regenerate):
    args = ctx.actions.args()
    args.set_param_file_format("multiline")
    args.use_param_file("@%s", use_always = True)

    inputs = []
    outputs = []

    # Append the module root
    module_root = ctx.build_file_path.replace("/BUILD", "")
    args.add("--module-root", module_root)

    # Append the module name
    module_name = module_root.replace("/", "_").replace("-", "_")
    args.add("--module-name", "{}_{}".format(module_name, ctx.attr.name))

    # Append the label
    self = str(ctx.label).replace("@//", "//")
    args.add("--label", "{}".format(self))

    # Append the extra information that Lint needs to know about our byte code
    args.add("--compile-sdk-version", "33")
    args.add("--java-language-level", "1.8")
    args.add("--kotlin-language-level", "1.8")

    # Append the input source files
    for src in ctx.files.srcs:
        args.add("--src", src)
    inputs.extend(ctx.files.srcs)

    # Append the input source files
    for res in ctx.files.resource_files:
        args.add("--resource", res)
    inputs.extend(ctx.files.resource_files)

    # Append the Android manifest file
    if ctx.attr.manifest:
        manifest = ctx.file.manifest
        if ctx.file.manifest.basename != "AndroidManifest.xml":
            manifest = ctx.actions.declare_file("AndroidManifest.xml")
            ctx.actions.symlink(output = manifest, target_file = ctx.file.manifest)
        args.add("--android-manifest", manifest)
        inputs.append(manifest)

    # Append the baseline file
    if not regenerate and ctx.attr.baseline:
        args.add("--baseline-file", ctx.file.baseline)
        inputs.append(ctx.file.baseline)
    if regenerate:
        args.add("--regenerate-baseline-files")

    # Append the configuration
    if ctx.attr.config:
        args.add("--config-file", ctx.file.config)
        inputs.append(ctx.file.config)

    # Append the configuration
    if ctx.attr.warnings_as_errors:
        args.add("--warnings-as-errors")

    # Append the custom rules
    for jar in ctx.files.custom_rules:
        args.add("--custom-rule", jar)
    inputs.extend(ctx.files.custom_rules)

    # Collect the transitive classpath jars to run lint against. The compile jars are intentionally picked
    # because they are ABI jars which are much more light weight for lint to load
    classpath_jars = []
    classpath_aars = []
    for dep in ctx.attr.deps:
        if JavaInfo in dep:
            classpath_jars.append(dep[JavaInfo].compile_jars)
        if AndroidLibraryResourceClassJarProvider in dep:
            classpath_jars.append(dep[AndroidLibraryResourceClassJarProvider].jars)
        if AndroidLibraryAarInfo in dep:
            classpath_aars.append(dep[AndroidLibraryAarInfo].transitive_aar_artifacts)
        if AndroidLintAARInfo in dep:
            classpath_aars.append(dep[AndroidLintAARInfo].aars)

    # Append the compiled R files for our self
    if ctx.attr.self:
        if AndroidLibraryResourceClassJarProvider in ctx.attr.self:
            classpath_jars.append(ctx.attr.self[AndroidLibraryResourceClassJarProvider].jars)

    # Append the classpath inputs
    for jar in depset(transitive = classpath_jars).to_list():
        args.add("--classpath", jar)
        inputs.append(jar)

    # Append the aar inputs
    for aar in depset(transitive = classpath_aars).to_list():
        if not aar.path.endswith(".aar"):
            continue
        args.add("--classpath", aar)
        inputs.append(aar)

    # Enable auto fix
    if ctx.attr.autofix == True:
        args.add("--autofix")

    # Append the disabled checks
    for check in ctx.attr.disable_checks:
        args.add("--disable-check", check)

    # Append the enabled checks
    for check in ctx.attr.enable_checks:
        args.add("--enable-check", check)

    # Declare the output file
    output = ctx.actions.declare_file("{}.xml".format(ctx.label.name))
    args.add("--output", output.path)
    outputs.append(output)

    # Declare the project output file
    # This is mostly used for debugging Lint execution
    project_config = ctx.actions.declare_file("{}_project_config.xml".format(ctx.label.name))
    args.add("--project-config-output", project_config.path)
    outputs.append(project_config)

    ctx.actions.run(
        mnemonic = "AndroidLint",
        inputs = inputs,
        outputs = outputs,
        executable = ctx.executable._lint_wrapper,
        execution_requirements = {},
        progress_message = "Running Android Lint {}".format(str(ctx.label)),
        arguments = [args],
        tools = [ctx.executable._lint_wrapper],
    )

    return struct(
        lint_baseline = output,
        lint_project_configuration = project_config,
    )

def _test_impl(ctx):
    providers = _collect_android_lint_providers(ctx, regenerate = False)

    inputs = []
    inputs.append(providers.lint_baseline)
    inputs.extend(ctx.attr._test_runner_executable.default_runfiles.files.to_list())

    ctx.actions.expand_template(
        template = ctx.file._test_runner_template,
        output = ctx.outputs.executable,
        is_executable = True,
        substitutions = {
            "{executable}": ctx.executable._test_runner_executable.short_path,
            "{lint_baseline}": providers.lint_baseline.short_path,
            "{regenerate_baseline_files}": "false",
        },
    )

    return [
        AndroidLintResultsInfo(
            output = providers.lint_baseline,
            project_configuration = providers.lint_project_configuration,
        ),
        DefaultInfo(
            runfiles = ctx.runfiles(files = inputs),
            executable = ctx.outputs.executable,
            files = depset([
                ctx.outputs.executable,
                providers.lint_baseline,
                providers.lint_project_configuration,
            ]),
        ),
    ]

lint_test = rule(
    implementation = _test_impl,
    attrs = dicts.add(
        _ATTRS,
        {
            "baseline": attr.label(
                mandatory = False,
                allow_single_file = True,
                doc = "Lint baseline file.",
            ),
        },
    ),
    provides = [DefaultInfo, AndroidLintResultsInfo],
    test = True,
)
