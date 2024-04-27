"""Extensions for bzlmod.
"""

load(":repositories.bzl", "android_lint_register_toolchains")

def _toolchain_extension(_):
    android_lint_register_toolchains()

android_lint = module_extension(
    implementation = _toolchain_extension,
)
