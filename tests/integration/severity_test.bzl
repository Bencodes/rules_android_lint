"""Check both outputs of android_lint_test on the DefaultLocale fixture."""

load("@rules_python//python:defs.bzl", "py_test")
load("//rules:defs.bzl", "android_lint_test")

def severity_test(name, expected_exit_code, expected_severity = None, **kwargs):
    """Run android_lint_test and check its report and exit status together.

    Args:
        name: Name of the assertion target.
        expected_exit_code: Expected status of the lint test executable.
        expected_severity: Expected DefaultLocale severity, or None for a clean report.
        **kwargs: Additional attributes of android_lint_test.
    """
    subject = name + "_subject"
    android_lint_test(
        name = subject,
        srcs = [":basic_dirty_lib"],
        lib = ":basic_dirty_lib",
        # The promoted-warning case is expected to exit 1 when run directly.
        tags = ["manual"],
        **kwargs
    )
    args = ["--expected-exit-code", str(expected_exit_code)]
    if expected_severity:
        args.extend(["--expected-severity", expected_severity])
    py_test(
        name = name,
        srcs = ["severity_test.py"],
        main = "severity_test.py",
        args = args + ["$(rlocationpath :%s)" % subject],
        data = [":" + subject],
        deps = ["@rules_python//python/runfiles"],
    )
