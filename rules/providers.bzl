"""Providers used by the Android Lint rules
"""

AndroidLintResultsInfo = provider(
    "Info needed to evaluate lint results",
    fields = {
        "baseline": "The Android Lint baseline output",
        "xml_output": "The Android Lint xml output",
        "html_output": "The Android Lint html output",
    },
)
