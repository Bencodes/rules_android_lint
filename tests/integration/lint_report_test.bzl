"""Test macro for making assertions against an android_lint XML report."""

load("@rules_python//python:defs.bzl", "py_test")

_REPORT_ASSERTION = Label("//tests/integration:report_assertion.py")

def lint_report_test(
        name,
        lint,
        expected_issues = [],
        rejected_issues = [],
        **kwargs):
    """Defines a portable Python test that checks an android_lint report.

    Args:
        name: Name of the generated test.
        lint: android_lint target whose XML output should be checked.
        expected_issues: Lint issue IDs that must be present.
        rejected_issues: Lint issue IDs that must be absent.
        **kwargs: Additional arguments forwarded to py_test.
    """
    arguments = [
        "--report",
        "$(rlocationpath {})".format(lint),
    ]
    for issue in expected_issues:
        arguments.extend(["--expect-issue", issue])
    for issue in rejected_issues:
        arguments.extend(["--reject-issue", issue])

    py_test(
        name = name,
        srcs = [_REPORT_ASSERTION],
        main = _REPORT_ASSERTION,
        args = arguments,
        data = [lint],
        deps = ["@rules_python//python/runfiles"],
        **kwargs
    )
