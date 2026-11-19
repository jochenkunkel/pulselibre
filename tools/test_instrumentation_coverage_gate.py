"""Offline regression tests for the Android instrumentation coverage gate."""
import pathlib
import subprocess
import tempfile
import unittest

ROOT = pathlib.Path(__file__).resolve().parents[1]
SCRIPT = ROOT / ".github" / "scripts" / "verify-instrumentation-coverage.sh"


class InstrumentationCoverageGateTests(unittest.TestCase):
    def check(self, filename=None, contents=b""):
        with tempfile.TemporaryDirectory() as directory:
            root = pathlib.Path(directory)
            if filename:
                target = root / "outputs" / "code_coverage" / "debugAndroidTest" / filename
                target.parent.mkdir(parents=True, exist_ok=True)
                target.write_bytes(contents)
            return subprocess.run(
                ["bash", str(SCRIPT), str(root)],
                capture_output=True, text=True, timeout=10,
            )

    def test_missing_directory_fails(self):
        self.assertNotEqual(0, self.check().returncode)

    def test_empty_ec_file_fails(self):
        self.assertNotEqual(0, self.check("test.ec").returncode)

    def test_nonempty_ec_file_passes(self):
        self.assertEqual(0, self.check("test.ec", b"jacoco").returncode)

    def test_unrelated_report_does_not_pass(self):
        self.assertNotEqual(0, self.check("report.html", b"not coverage").returncode)


if __name__ == "__main__":
    unittest.main()
