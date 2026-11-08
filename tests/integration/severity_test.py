import argparse
import subprocess
import sys
from xml.etree import ElementTree

from python.runfiles import runfiles


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("--expected-exit-code", type=int, required=True)
    parser.add_argument("--expected-severity")
    parser.add_argument("lint_test")
    args = parser.parse_args()

    runfiles_tree = runfiles.Create()
    executable = runfiles_tree.Rlocation(args.lint_test)
    report = runfiles_tree.Rlocation(args.lint_test + ".xml")
    root = ElementTree.parse(report).getroot()
    issues = [(issue.get("id"), issue.get("severity")) for issue in root.findall("issue")]
    expected = [("DefaultLocale", args.expected_severity)] if args.expected_severity else []
    if root.tag != "issues" or issues != expected:
        print(f"Expected <issues> with {expected}, got <{root.tag}> with {issues}", file=sys.stderr)
        return 1

    status = subprocess.run(["bash", executable]).returncode
    if status != args.expected_exit_code:
        print(f"Report contained {issues}. Expected exit {args.expected_exit_code}, got {status}", file=sys.stderr)
        return 1
    return 0


if __name__ == "__main__":
    sys.exit(main())
