import json
import os
from pathlib import Path
import signal
import subprocess
import sys
import tempfile
import time
import unittest


SCRIPT = Path(__file__).with_name("s25_lease.py")


class DeviceLeaseTests(unittest.TestCase):
    def setUp(self):
        self.tmp = tempfile.TemporaryDirectory()
        self.root = Path(self.tmp.name)
        self.env = os.environ.copy()
        self.env["HOME"] = str(self.root / "home")
        self.env.pop("PULSELIBRE_S25_LOCK_DIR", None)
        self.env.pop("PULSELIBRE_S10_LOCK_DIR", None)

    def tearDown(self):
        self.tmp.cleanup()

    def cli(self, *args, **kwargs):
        return subprocess.run(
            [sys.executable, str(SCRIPT), *args],
            env=self.env,
            text=True,
            capture_output=True,
            timeout=10,
            **kwargs,
        )

    def run_lease(self, *args, command=None):
        return [
            sys.executable,
            str(SCRIPT),
            "run",
            *args,
            "--",
            sys.executable,
            "-c",
            command or "import time; time.sleep(2)",
        ]

    def test_legacy_cli_defaults_to_s25_and_preserves_override(self):
        s25 = self.root / "shared-s25"
        self.env["PULSELIBRE_S25_LOCK_DIR"] = str(s25)
        proc = subprocess.Popen(
            self.run_lease("--agent", "legacy", "--minutes", "1"),
            env=self.env,
            text=True,
            stdout=subprocess.PIPE,
            stderr=subprocess.PIPE,
        )
        try:
            for _ in range(100):
                if (s25 / "status.json").exists():
                    break
                time.sleep(0.01)
            self.assertTrue((s25 / "status.json").exists())
            status = self.cli("status")
            self.assertEqual(status.returncode, 1)
            self.assertIn("BELEGT (S25)", status.stdout)
        finally:
            proc.terminate()
            proc.communicate(timeout=5)

    def test_different_devices_are_independent_and_status_redacts_serial(self):
        self.env["PULSELIBRE_S10_SERIAL"] = "RF8M-private"
        s10 = subprocess.Popen(
            self.run_lease("--device", "RF8M-private", "--agent", "gui", "--minutes", "1",
                           "--task", "smoke", "--session-id", "abc", "--test-class", "ui",
                           "--phase", "install"),
            env=self.env,
            stdout=subprocess.PIPE,
            stderr=subprocess.PIPE,
            text=True,
        )
        try:
            base = Path(self.env["HOME"]) / ".cache/pulselibre"
            s10_status = base / "s10-lease/status.json"
            for _ in range(100):
                if s10_status.exists():
                    break
                time.sleep(0.01)
            self.assertTrue(s10_status.exists())
            self.assertEqual(self.cli("status", "--device", "s10").returncode, 1)
            self.assertEqual(self.cli("status", "--device", "s25").returncode, 0)
            record = json.loads(s10_status.read_text())
            self.assertEqual(record["device"], "s10")
            self.assertEqual(record["task"], "smoke")
            self.assertEqual(record["session_id"], "abc")
            self.assertEqual(record["test_class"], "ui")
            self.assertEqual(record["phase"], "install")
            self.assertNotIn("RF8M-private", s10_status.read_text())
            self.assertNotIn("RF8M-private", self.cli("status", "--device", "s10").stdout)
        finally:
            s10.terminate()
            s10.communicate(timeout=5)

    def test_competing_agents_only_one_acquires_same_device(self):
        holder = subprocess.Popen(
            self.run_lease("--device", "s10", "--agent", "first", "--minutes", "1"),
            env=self.env,
            stdout=subprocess.PIPE,
            stderr=subprocess.PIPE,
            text=True,
        )
        try:
            base = Path(self.env["HOME"]) / ".cache/pulselibre/s10-lease"
            for _ in range(100):
                if (base / "status.json").exists():
                    break
                time.sleep(0.01)
            contender = self.cli(
                "run", "--device", "s10", "--agent", "second", "--minutes", "1",
                "--", sys.executable, "-c", "pass",
            )
            self.assertEqual(contender.returncode, 2)
            self.assertIn("S10 BELEGT", contender.stderr)
        finally:
            holder.terminate()
            holder.communicate(timeout=5)

    def test_eta_is_not_a_timeout_and_stale_json_does_not_claim_lock(self):
        base = Path(self.env["HOME"]) / ".cache/pulselibre/s10-lease"
        base.mkdir(parents=True)
        status_path = base / "status.json"
        status_path.write_text(json.dumps({"agent": "stale", "expected_free_utc": "2000-01-01"}))
        status = self.cli("status", "--device", "s10")
        self.assertEqual(status.returncode, 0)
        self.assertIn("veraltete Statusdatei ignoriert", status.stdout)

        proc = subprocess.Popen(
            self.run_lease("--device", "s10", "--agent", "long", "--minutes", "1",
                           command="import time; time.sleep(2)"),
            env=self.env,
            stdout=subprocess.PIPE,
            stderr=subprocess.PIPE,
            text=True,
        )
        try:
            for _ in range(100):
                try:
                    record = json.loads(status_path.read_text())
                except (OSError, ValueError):
                    record = {}
                if record.get("agent") == "long":
                    break
                time.sleep(0.01)
            self.assertEqual(record.get("agent"), "long")
            self.assertEqual(self.cli("status", "--device", "s10").returncode, 1)
            # Simulate an already elapsed ETA while the owner still holds flock.
            record["expected_free_utc"] = "2000-01-01T00:00:00+00:00"
            status_path.write_text(json.dumps(record))
            self.assertEqual(self.cli("status", "--device", "s10").returncode, 1)
            self.assertEqual(proc.wait(timeout=3), 0)
            proc.communicate(timeout=1)
            self.assertFalse(status_path.exists())
        finally:
            if proc.poll() is None:
                proc.terminate()
            proc.communicate(timeout=5)

    def test_sigterm_cleans_up_child_process_group_and_status(self):
        base = Path(self.env["HOME"]) / ".cache/pulselibre/s10-lease"
        pid_file = self.root / "child.pid"
        child_code = (
            "import subprocess,sys,time; "
            "p=subprocess.Popen([sys.executable,'-c','import time; time.sleep(60)']); "
            f"open({str(pid_file)!r},'w').write(str(p.pid)); "
            "time.sleep(60)"
        )
        proc = subprocess.Popen(
            self.run_lease("--device", "s10", "--agent", "signal", "--minutes", "1",
                           command=child_code),
            env=self.env,
            stdout=subprocess.PIPE,
            stderr=subprocess.PIPE,
            text=True,
        )
        try:
            for _ in range(200):
                if pid_file.exists():
                    break
                time.sleep(0.01)
            self.assertTrue(pid_file.exists())
            child_pid = int(pid_file.read_text())
            proc.send_signal(signal.SIGTERM)
            proc.communicate(timeout=5)
            self.assertEqual(proc.returncode, 143)
            self.assertFalse((base / "status.json").exists())
            with self.assertRaises(ProcessLookupError):
                os.kill(child_pid, 0)
        finally:
            if proc.poll() is None:
                proc.kill()
                proc.communicate(timeout=5)

    def test_worktrees_share_home_lock_and_eta_is_metadata_only(self):
        # Separate script invocations/worktree paths still resolve the same
        # default lock because it is rooted in the shared host user's HOME.
        first = subprocess.Popen(
            self.run_lease("--device", "s10", "--agent", "worktree-a", "--minutes", "2",
                           command="import time; time.sleep(60)"),
            env=self.env,
            stdout=subprocess.PIPE,
            stderr=subprocess.PIPE,
        )
        try:
            for _ in range(100):
                try:
                    record = json.loads((Path(self.env["HOME"]) / ".cache/pulselibre/s10-lease/status.json").read_text())
                except (OSError, ValueError):
                    record = {}
                if record.get("agent") == "worktree-a":
                    break
                time.sleep(0.01)
            self.assertEqual(record.get("agent"), "worktree-a")
            self.assertEqual(self.cli("status", "--device", "s10").returncode, 1)
            blocked = self.cli(
                "run", "--device", "s10", "--agent", "worktree-b", "--minutes", "1",
                "--", sys.executable, "-c", "pass",
            )
            self.assertEqual(blocked.returncode, 2)
        finally:
            first.terminate()
            first.communicate(timeout=5)


if __name__ == "__main__":
    unittest.main()
