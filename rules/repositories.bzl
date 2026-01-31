"""Declare runtime dependencies.

Dependencies are declared in MODULE.bazel via Bzlmod.
"""

def android_lint_register_toolchains(name = "android_lint_toolchains"):
    """Convenience macro for users which does typical setup.

    This registers the default android_lint toolchain.

    Args:
        name: Name for this macro invocation (unused, required by convention).
    """
    native.register_toolchains("@rules_android_lint//toolchains:android_lint_default_toolchain")
