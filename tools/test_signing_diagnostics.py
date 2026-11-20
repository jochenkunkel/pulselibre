"""Regression coverage for signing state in the separate diagnostics phase."""

import pathlib
import re
import unittest

ROOT = pathlib.Path(__file__).resolve().parents[1]
RUN_CI = ROOT / "tools" / "ci" / "run-ci.sh"


class SigningDiagnosticsTests(unittest.TestCase):
    def test_diagnostics_restores_signing_before_reporting_status(self):
        source = RUN_CI.read_text(encoding="utf-8")
        diagnostics = re.search(
            r"^diagnostics\(\) \{\n(?P<body>.*?)(?=^\})",
            source,
            re.MULTILINE | re.DOTALL,
        )
        self.assertIsNotNone(diagnostics, "diagnostics() function not found")
        body = diagnostics.group("body")
        restore = body.find("ci_signing_restore")
        status = body.find("ci_signing_status")
        self.assertGreaterEqual(restore, 0, "diagnostics must restore signing config")
        self.assertGreater(status, restore, "signing status must follow restore")
        self.assertIn('echo "APK_SIGNING_READY=true"', body)
        self.assertIn('echo "APK_SIGNING_READY=false"', body)


if __name__ == "__main__":
    unittest.main()
