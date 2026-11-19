"""Regression tests for the CI Gradle 429 retry wrapper (issue #156)."""
import os
import pathlib
import subprocess
import tempfile
import unittest

ROOT = pathlib.Path(__file__).resolve().parents[1]
WRAPPER = ROOT / ".github" / "scripts" / "gradle-retry.sh"


class GradleRetryTests(unittest.TestCase):
    def run_case(self, responses):
        with tempfile.TemporaryDirectory() as directory:
            root = pathlib.Path(directory)
            fake = root / "gradle"
            fake.write_text(
                "#!/usr/bin/env bash\n"
                "count_file=\"$FAKE_COUNT\"\n"
                "n=$(cat \"$count_file\" 2>/dev/null || echo 0)\n"
                "n=$((n + 1)); echo \"$n\" > \"$count_file\"\n"
                "case \"$n\" in\n"
                + "".join(
                    f" {i}) echo {message!r}; exit {exit_code};;\n"
                    for i, (message, exit_code) in enumerate(responses, 1)
                )
                + " *) echo unexpected-attempt; exit 98;;\n"
                "esac\n"
            )
            fake.chmod(0o755)
            sleep = root / "sleep"
            sleep.write_text("#!/usr/bin/env bash\nexit 0\n")
            sleep.chmod(0o755)
            count = root / "attempts"
            env = os.environ.copy()
            env["PATH"] = f"{root}:{env.get('PATH', '')}"
            env["FAKE_COUNT"] = str(count)
            result = subprocess.run(
                ["bash", str(WRAPPER), "assembleDebug"],
                env=env, capture_output=True, text=True, timeout=20,
            )
            return result, int(count.read_text())

    def test_success_without_retry(self):
        result, attempts = self.run_case([("BUILD SUCCESSFUL", 0)])
        self.assertEqual(0, result.returncode)
        self.assertEqual(1, attempts)

    def test_http_429_retries_then_succeeds(self):
        result, attempts = self.run_case([
            ("HTTP 429 Too Many Requests", 1),
            ("BUILD SUCCESSFUL", 0),
        ])
        self.assertEqual(0, result.returncode)
        self.assertEqual(2, attempts)

    def test_compiler_failure_never_retries(self):
        result, attempts = self.run_case([("Compilation failed", 2)])
        self.assertEqual(2, result.returncode)
        self.assertEqual(1, attempts)

    def test_repeated_throttling_stops_after_three_attempts(self):
        result, attempts = self.run_case([
            ("status code 429", 1),
            ("response code: 429", 1),
            ("Too Many Requests", 1),
        ])
        self.assertEqual(1, result.returncode)
        self.assertEqual(3, attempts)


if __name__ == "__main__":
    unittest.main()
