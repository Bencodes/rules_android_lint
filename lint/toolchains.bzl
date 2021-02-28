"""
toolchains requirements to expose in rules_android_lint
"""

load("@rules_android_lint//lint/internal:toolchains.bzl", _rules_android_lint_toolchains = "rules_android_lint_toolchains")

rules_android_lint_toolchains = _rules_android_lint_toolchains
