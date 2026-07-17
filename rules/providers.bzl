"""Providers used by the Android Lint rules
"""

AndroidLintResultsInfo = provider(
    "Info needed to evaluate lint results",
    fields = {
        "output": "The Android Lint baseline output",
    },
)

AndroidLintPartialResultsInfo = provider(
    "Per-target Android Lint analysis (--analyze-only) outputs, propagated up the dependency " +
    "graph so a leaf rule can run the report (--report-only) phase over the transitive set.",
    fields = {
        "is_android": "Whether this target is an Android module.",
        "manifest": "The direct Android manifest used for this target's analysis, or None.",
        "partial_results": "Directory File of this target's partial results, or None if this " +
                           "node had no lintable inputs.",
        "resource_files": "Depset of direct Android resources used for this target's analysis.",
        "module_name": "The lint module name for this target, or None if not analyzed.",
        "transitive_results": "depset of structs(module_name, partial_results, is_android) for " +
                              "this target and all analyzed transitive dependencies.",
    },
)
