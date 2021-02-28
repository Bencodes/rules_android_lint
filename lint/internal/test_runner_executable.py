import argparse
import sys
from typing import Any
from typing import Dict
from typing import List
from xml.etree import ElementTree


def __string_to_bool(s) -> bool:
    return s.lower() in ("yes", "true", "1")


def __validate_output(lint_baseline) -> int:
    # Read in the lint baseline file
    with open(lint_baseline, "r") as f:
        output = f.read()

    # Parse it into an actual array of issues
    output_parsed = __parse_android_lint(output)

    # Look at the flagged issues and categorize them
    # issues - valid issues that need to be fixed or baselined
    # issues_fixed - issues that have been fixed but are still in the baseline
    # issues_ignored - issues that exist but are in the baseline file (ignored)
    issues = []
    issues_fixed = []
    issues_ignored = []
    for issue in output_parsed:
        message = issue["message"]
        if issue["id"] == "LintBaseline":
            issues_ignored.append(issue)
        elif "perhaps they have been fixed" in message:
            issues_fixed.append(issue)
        else:
            issues.append(issue)

    if len(issues) > 0:
        print(output)
        return 1
    return 0


def __parse_android_lint(input: str) -> List[Dict[str, Any]]:
    results = []  # type: List[Dict[str, Any]]

    try:
        root = ElementTree.fromstring(input)
    except ElementTree.ParseError:
        return results

    for issue in root.findall("issue"):
        issues = {"id": issue.get("id", None), "message": issue.get("message", None)}
        results.append({k: v for k, v in issues.items() if v})
    return results


def __main(lint_baseline, regenerate_baseline_files):
    if regenerate_baseline_files:
        return 0

    return __validate_output(lint_baseline=lint_baseline)


if __name__ == "__main__":
    # Parse the arguments
    parser = argparse.ArgumentParser(prog="lint_parse")
    parser.add_argument("--lint_baseline", required=True)
    parser.add_argument("--regenerate_baseline_files", required=False, default=False)
    args = parser.parse_args(sys.argv[1:])

    # Run it and exit with the proper exit code
    exit_code = __main(
        lint_baseline=args.lint_baseline,
        regenerate_baseline_files=__string_to_bool(args.regenerate_baseline_files),
    )
    sys.exit(exit_code)
