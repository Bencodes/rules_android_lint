"""Android Lint Toolchain."""

_ATTRS = dict(
    enable_check_dependencies = attr.bool(
        default = False,
        doc = """Enables the dependency analysis features within lint. Warning: This feature is extremely
        expensive and will slow down Lint. It's recommended that you leave this feature disabled unless it's
        explicitly required.
        """,
    ),
    compile_sdk_version = attr.string(
        default = "34",
        doc = "The Android SDK version to compile against.",
    ),
    java_language_level = attr.string(
        default = "1.8",
        doc = "The Java language level to compile against.",
    ),
    kotlin_language_level = attr.string(
        default = "1.8",
        doc = "The Kotlin language level to compile against.",
    ),
    android_lint_config = attr.label(
        doc = "The Android Lint config file to use globally.",
        allow_single_file = True,
        cfg = "exec",
    ),
)

def _impl(ctx):
    return [platform_common.ToolchainInfo(
        **{name: getattr(ctx.attr, name) for name in _ATTRS.keys()}
    )]

android_lint_toolchain = rule(
    implementation = _impl,
    attrs = _ATTRS,
)
