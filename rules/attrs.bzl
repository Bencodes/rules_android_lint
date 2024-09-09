"""Generic attributes used by the Android Lint rules
"""

load(
    ":collect_aar_outputs_aspect.bzl",
    _collect_aar_outputs_aspect = "collect_aar_outputs_aspect",
)

ATTRS = dict(
    _lint_wrapper = attr.label(
        default = Label("//src/cli"),
        executable = True,
        cfg = "exec",
    ),
    srcs = attr.label_list(
        mandatory = True,
        allow_files = [".java", ".kt", ".kts"],
        allow_empty = True,
        doc = "Sources to run Android Lint against.",
    ),
    resource_files = attr.label_list(
        mandatory = False,
        allow_files = [".xml"],
        allow_empty = True,
        default = [],
        doc = "Android resource files to run Android Lint against.",
    ),
    lib = attr.label(
        mandatory = True,
        doc = "The target being linted. This is needed to get the compiled R files.",
    ),
    manifest = attr.label(
        mandatory = False,
        allow_single_file = True,
        doc = "Android manifest to run Android Lint against.",
    ),
    deps = attr.label_list(
        mandatory = False,
        allow_empty = True,
        default = [],
        aspects = [_collect_aar_outputs_aspect],
        doc = "Dependencies that should be on the classpath during execution.",
    ),
    android_lint_config = attr.label(
        mandatory = False,
        allow_single_file = True,
        doc = "Lint Android Lint configuration file.",
    ),
    is_test_sources = attr.bool(
        default = False,
        doc = "True when linting test sources, otherwise false.",
    ),
    autofix = attr.bool(
        default = False,
        doc = "Enables lint autofix. This is a no-op right now.",
    ),
    warnings_as_errors = attr.bool(
        default = False,
        doc = "When true, lint will treat warnings as errors.",
    ),
    disable_checks = attr.string_list(
        mandatory = False,
        allow_empty = True,
        default = [],
        doc = "List of checks to disable.",
    ),
    enable_checks = attr.string_list(
        mandatory = False,
        allow_empty = True,
        default = [],
        doc = "List of checks to enable.",
    ),
    custom_rules = attr.label_list(
        mandatory = False,
        allow_empty = True,
        allow_files = True,
        default = [],
        doc = "Custom lint rules to run.",
    ),
    output_formats = attr.string_list(
        mandatory = False,
        allow_empty = False,
        default = ["xml"],
        doc = "List of output formats to produce. Supported [xml, html]",
    ),
    _use_auto_exec_groups = attr.bool(default = True),
)
