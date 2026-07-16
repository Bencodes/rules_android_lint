import argparse
import sys
from typing import Any
from typing import Dict
from typing import List
from xml.etree import ElementTree


def validate_output(lint_baseline: str) -> int:
    # Read in the lint baseline file
    with open(lint_baseline, "r") as f:
        output = f.read()

    # Parse it into an actual array of issues. A missing or malformed report must
    # fail the test rather than being mistaken for a clean lint result.
    try:
        output_parsed = parse_android_lint(output)
    except (ElementTree.ParseError, ValueError) as error:
        print(f"Unable to parse Android lint report {lint_baseline}: {error}", file=sys.stderr)
        return 1

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


def parse_android_lint(report: str) -> List[Dict[str, Any]]:
    results = []  # type: List[Dict[str, Any]]

    root = ElementTree.fromstring(report)
    if root.tag != "issues":
        raise ValueError(f"expected <issues> root element, found <{root.tag}>")

    for issue in root.findall("issue"):
        issue_id = issue.get("id")
        message = issue.get("message")
        if not issue_id or message is None:
            raise ValueError("expected each <issue> to have id and message attributes")
        results.append({"id": issue_id, "message": message})
    return results


def main(lint_baseline: str) -> int:
    return validate_output(lint_baseline=lint_baseline)


if __name__ == "__main__":
    # Parse the arguments
    parser = argparse.ArgumentParser(prog="lint_parse")
    parser.add_argument("--lint_baseline", required=True)
    args = parser.parse_args(sys.argv[1:])

    # Run it and exit with the proper exit code
    exit_code = main(
        lint_baseline=args.lint_baseline,
    )
    sys.exit(exit_code)
