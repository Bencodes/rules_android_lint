"""
Rules defined and exposed by rules_android_lint
"""

load("@rules_android_lint//lint/internal:defs.bzl", _lint_test = "lint_test")

lint_test = _lint_test
