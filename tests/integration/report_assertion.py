import argparse
import sys
from xml.etree import ElementTree

from python.runfiles import runfiles


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("--report", required=True)
    parser.add_argument("--expect-issue", action="append", default=[])
    parser.add_argument("--expect-location-suffix", action="append", default=[])
    parser.add_argument("--reject-issue", action="append", default=[])
    args = parser.parse_args()

    runfiles_tree = runfiles.Create()
    report_path = runfiles_tree.Rlocation(args.report) if runfiles_tree else None
    if not report_path:
        print(f"Unable to locate lint report runfile: {args.report}", file=sys.stderr)
        return 1

    try:
        root = ElementTree.parse(report_path).getroot()
    except (ElementTree.ParseError, OSError) as error:
        print(f"Unable to parse lint report {args.report}: {error}", file=sys.stderr)
        return 1
    if root.tag != "issues":
        print(f"Invalid lint report root element: <{root.tag}>", file=sys.stderr)
        return 1

    issue_ids = {issue.get("id") for issue in root.findall("issue") if issue.get("id")}
    location_files = {
        location.get("file")
        for location in root.findall(".//location")
        if location.get("file")
    }

    missing = sorted(set(args.expect_issue) - issue_ids)
    missing_locations = sorted(
        suffix
        for suffix in set(args.expect_location_suffix)
        if not any(location.endswith(suffix) for location in location_files)
    )
    unexpected = sorted(set(args.reject_issue) & issue_ids)
    if missing or missing_locations or unexpected:
        if missing:
            print(f"Missing expected lint issues: {', '.join(missing)}", file=sys.stderr)
        if missing_locations:
            print(
                f"Missing expected lint location suffixes: {', '.join(missing_locations)}",
                file=sys.stderr,
            )
        if unexpected:
            print(f"Found rejected lint issues: {', '.join(unexpected)}", file=sys.stderr)
        print(f"Actual lint issues: {', '.join(sorted(issue_ids))}", file=sys.stderr)
        print(f"Actual lint locations: {', '.join(sorted(location_files))}", file=sys.stderr)
        return 1
    return 0


if __name__ == "__main__":
    sys.exit(main())
