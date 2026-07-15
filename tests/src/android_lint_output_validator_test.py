import contextlib
import io
import tempfile
import unittest
from pathlib import Path

from src.android_lint_output_validator import validate_output


class AndroidLintOutputValidatorTest(unittest.TestCase):
    def test_clean_report_passes(self) -> None:
        self.assertEqual(0, self._validate("<issues/>"))

    def test_regular_issue_fails_and_prints_report(self) -> None:
        report = '<issues><issue id="DefaultLocale" message="Use Locale.ROOT"/></issues>'
        output = io.StringIO()

        with contextlib.redirect_stdout(output):
            exit_code = self._validate(report)

        self.assertEqual(1, exit_code)
        self.assertIn("DefaultLocale", output.getvalue())

    def test_baseline_metadata_issue_is_ignored(self) -> None:
        report = '<issues><issue id="LintBaseline" message="Baseline information"/></issues>'
        self.assertEqual(0, self._validate(report))

    def test_fixed_baseline_issue_is_ignored(self) -> None:
        report = (
            '<issues><issue id="ObsoleteLintCustomCheck" '
            'message="Some baseline issues are missing; perhaps they have been fixed"/></issues>'
        )
        self.assertEqual(0, self._validate(report))

    def test_malformed_report_fails(self) -> None:
        output = io.StringIO()

        with contextlib.redirect_stderr(output):
            exit_code = self._validate("not xml")

        self.assertEqual(1, exit_code)
        self.assertIn("Unable to parse Android lint report", output.getvalue())

    def test_wrong_root_element_fails(self) -> None:
        output = io.StringIO()

        with contextlib.redirect_stderr(output):
            exit_code = self._validate("<report/>")

        self.assertEqual(1, exit_code)
        self.assertIn("expected <issues> root element", output.getvalue())

    def test_issue_without_required_attributes_fails(self) -> None:
        output = io.StringIO()

        with contextlib.redirect_stderr(output):
            exit_code = self._validate('<issues><issue id="DefaultLocale"/></issues>')

        self.assertEqual(1, exit_code)
        self.assertIn("expected each <issue> to have id and message attributes", output.getvalue())

    def _validate(self, report: str) -> int:
        with tempfile.TemporaryDirectory() as temp_dir:
            report_path = Path(temp_dir) / "lint.xml"
            report_path.write_text(report)
            return validate_output(str(report_path))


if __name__ == "__main__":
    unittest.main()
