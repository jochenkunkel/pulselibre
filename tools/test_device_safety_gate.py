"""Offline regression tests for the emulator-only instrumentation gate.

The S10 incident on 2026-11-22 showed that a direct call of the emulator phase
silently ran instrumentation tests on the attached physical phone when no
emulator was started. These tests exercise every branch of the guard with a
fake ``adb`` and no device access.
"""

from __future__ import annotations

import os
import pathlib
import shlex
import shutil
import subprocess
import tempfile
import unittest

ROOT = pathlib.Path(__file__).resolve().parents[1]
GUARD = ROOT / ".github" / "scripts" / "require-emulator-serial.sh"
RUN_CI = ROOT / "tools" / "ci" / "run-ci.sh"
BASH = shutil.which("bash") or "/bin/bash"

PHYSICAL = "RF8M826126Z\tdevice"
EMULATOR = "emulator-5554\tdevice"
DEVICES_HEADER = "List of devices attached\n"


class DeviceSafetyGateTests(unittest.TestCase):
    def run_guard(self, adb_output=DEVICES_HEADER, serial=None, adb_available=True):
        with tempfile.TemporaryDirectory() as directory:
            bin_dir = pathlib.Path(directory) / "bin"
            bin_dir.mkdir()
            # Only awk is needed from the outside; isolating PATH prevents the
            # real adb (and a real phone) from leaking into these offline tests.
            awk = shutil.which("awk")
            if awk is None:
                raise AssertionError("awk wird für den Gate-Test benötigt")
            (bin_dir / "awk").symlink_to(awk)
            if adb_available:
                adb = bin_dir / "adb"
                adb.write_text(
                    f"#!{BASH}\nprintf '%s' " + shlex.quote(adb_output) + "\n",
                    encoding="utf-8",
                )
                adb.chmod(0o755)
            env = os.environ.copy()
            env["PATH"] = str(bin_dir)
            env.pop("ANDROID_SERIAL", None)
            if serial is not None:
                env["ANDROID_SERIAL"] = serial
            return subprocess.run(
                [BASH, str(GUARD)],
                capture_output=True, text=True, timeout=10, env=env,
            )

    def test_explicit_emulator_serial_passes(self):
        # A physical phone may be attached; an explicit emulator serial wins.
        result = self.run_guard(DEVICES_HEADER + PHYSICAL + "\n", serial="emulator-5554")
        self.assertEqual(0, result.returncode, result.stderr)

    def test_explicit_physical_serial_fails(self):
        result = self.run_guard(DEVICES_HEADER + EMULATOR + "\n", serial="RF8M826126Z")
        self.assertNotEqual(0, result.returncode)
        self.assertIn("ANDROID_SERIAL", result.stderr + result.stdout)

    def test_visible_physical_device_without_serial_fails(self):
        result = self.run_guard(DEVICES_HEADER + PHYSICAL + "\n")
        self.assertNotEqual(0, result.returncode)
        self.assertIn("Physisches Android-Gerät sichtbar", result.stderr + result.stdout)

    def test_emulator_without_serial_passes(self):
        result = self.run_guard(DEVICES_HEADER + EMULATOR + "\n")
        self.assertEqual(0, result.returncode, result.stderr)

    def test_no_devices_passes(self):
        # No device means Gradle fails with "no connected device" – it cannot
        # accidentally pick a phone.
        result = self.run_guard(DEVICES_HEADER)
        self.assertEqual(0, result.returncode, result.stderr)

    def test_offline_physical_device_is_ignored(self):
        result = self.run_guard("List of devices attached\nRF8M826126Z\toffline\n")
        self.assertEqual(0, result.returncode, result.stderr)

    def test_missing_adb_fails_closed(self):
        result = self.run_guard(adb_available=False)
        self.assertNotEqual(0, result.returncode)
        self.assertIn("adb nicht gefunden", result.stderr + result.stdout)


class RunCiWiringTests(unittest.TestCase):
    def test_guard_is_called_before_instrumentation_tests(self):
        script = RUN_CI.read_text(encoding="utf-8")
        guard_at = script.find(".github/scripts/require-emulator-serial.sh")
        tests_at = script.find(":app:connectedDebugAndroidTest")
        self.assertNotEqual(-1, guard_at, "Schutzschranke fehlt in run-ci.sh")
        self.assertNotEqual(-1, tests_at, "Instrumentationsphase fehlt in run-ci.sh")
        self.assertLess(guard_at, tests_at, "Schutzschranke muss vor den Tests stehen")

    def test_no_inline_serial_bypass_remains(self):
        # The guard must be the single source of truth; an inline check could
        # drift away from the tested implementation.
        script = RUN_CI.read_text(encoding="utf-8")
        self.assertNotIn("ANDROID_SERIAL", script)
        self.assertIn("require-emulator-serial.sh", script)


if __name__ == "__main__":
    unittest.main()
