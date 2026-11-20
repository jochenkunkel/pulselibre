"""Regression tests for safe, non-registering runner-installer execution.

All external commands which could reach GitHub or mutate system state are
stubbed. These tests never use a real registration token, network, service,
runner account, or write under /opt.
"""

from __future__ import annotations

import os
import pathlib
import subprocess
import tempfile
import unittest


ROOT = pathlib.Path(__file__).resolve().parents[1]
INSTALLER = ROOT / "tools" / "runner" / "install-runner.sh"


class RunnerInstallerTests(unittest.TestCase):
    def setUp(self):
        self.temp = tempfile.TemporaryDirectory(prefix="pulselibre-runner-test-")
        self.addCleanup(self.temp.cleanup)
        self.root = pathlib.Path(self.temp.name)
        self.bin_dir = self.root / "bin"
        self.bin_dir.mkdir()
        self.calls_file = self.root / "calls.log"
        self._write_stub(
            "gh",
            """#!/usr/bin/env bash
set -eu
printf 'gh %s\\n' "$*" >>"$STUB_CALLS"
if [[ "$*" == *"repos/"* ]]; then
  printf '%s\\n' "${STUB_VISIBILITY:-public}"
  exit 0
fi
exit 1
""",
        )
        self._write_stub(
            "curl",
            """#!/usr/bin/env bash
set -eu
printf 'curl %s\\n' "$*" >>"$STUB_CALLS"
last="${@: -1}"
case "$last" in
  *.sha256) exit 22 ;;
esac
output=""
previous=""
for arg in "$@"; do
  if [ "$previous" = "-o" ]; then output="$arg"; fi
  previous="$arg"
done
if [ -n "$output" ]; then
  printf 'test-only fake runner archive\\n' >"$output"
fi
exit 0
""",
        )
        self._write_stub(
            "id",
            """#!/usr/bin/env bash
if [ "${1:-}" = "-u" ]; then
  printf '1000\\n'
elif [ "${1:-}" = "-un" ]; then
  printf 'test-user\\n'
else
  exec /usr/bin/id "$@"
fi
""",
        )
        self._write_stub(
            "mkdir",
            """#!/usr/bin/env bash
set -eu
printf 'mkdir %s\\n' "$*" >>"$STUB_CALLS"
exec /bin/mkdir "$@"
""",
        )
        self._write_stub(
            "chown",
            """#!/usr/bin/env bash
printf 'unexpected chown: %s\\n' "$*" >&2
exit 97
""",
        )
        for name in ("chmod", "useradd", "sudo", "systemctl"):
            self._write_stub(
                name,
                f"""#!/usr/bin/env bash
printf 'unexpected {name}: %s\\n' "$*" >&2
exit 96
""",
            )
        self._write_stub(
            "tar",
            """#!/usr/bin/env bash
printf 'unexpected tar: %s\\n' "$*" >&2
exit 98
""",
        )

    def _write_stub(self, name: str, content: str):
        path = self.bin_dir / name
        path.write_text(content, encoding="utf-8")
        path.chmod(0o755)

    def run_installer(self, *args, **overrides):
        env = os.environ.copy()
        env.update(
            {
                "PATH": f"{self.bin_dir}:{env.get('PATH', '/usr/bin:/bin')}",
                "HOME": str(self.root / "home"),
                "STUB_CALLS": str(self.calls_file),
                "PULSELIBRE_RUNNER_ROOT": str(self.root / "user-runner"),
                "XDG_CONFIG_HOME": str(self.root / "xdg-config"),
            }
        )
        env.pop("GITHUB_ACTIONS", None)
        for secret_name in ("RUNNER_TOKEN", "GH_TOKEN", "GITHUB_TOKEN"):
            env.pop(secret_name, None)
        env.update({key: str(value) for key, value in overrides.items()})
        return subprocess.run(
            ["bash", str(INSTALLER), *args],
            cwd=ROOT,
            env=env,
            capture_output=True,
            text=True,
            timeout=30,
            check=False,
        )

    def test_system_dry_run_without_root_never_mutates_system_paths(self):
        result = self.run_installer(
            "--system",
            "--dry-run",
            "--allow-public",
            "--runner-version",
            "2.338.2",
        )

        self.assertEqual(0, result.returncode, result.stderr)
        self.assertIn("Dry-Run ohne root", result.stderr)
        self.assertIn("kein Registrierungs-Token erforderlich oder abgerufen", result.stderr)
        # The self-hosted runner invokes this test from its own configured
        # installation. In that case the installer must preserve the existing
        # registration instead of printing a fresh config command. Hosted CI
        # exercises the fresh-install dry-run branch.
        configured_runner = pathlib.Path("/opt/pulselibre-runner/.runner").exists()
        if configured_runner:
            self.assertNotIn("sudo -u ghrunner ./config.sh", result.stdout)
        else:
            self.assertIn("sudo -u ghrunner ./config.sh", result.stdout)
        self.assertNotIn("DRY-RUN-NO-TOKEN", result.stdout + result.stderr)
        calls = self.calls_file.read_text(encoding="utf-8")
        self.assertNotIn("mkdir ", calls)
        # The configured system runner may already have the requested archive
        # version, in which case a dry-run correctly skips the download.
        self.assertIn("[dry-run]", result.stdout)
        self.assertNotIn("registration-token", calls)

    def test_user_dry_run_is_confined_to_test_temp_directory(self):
        result = self.run_installer(
            "--user",
            "--dry-run",
            "--allow-public",
            "--runner-version",
            "2.338.2",
        )

        self.assertEqual(0, result.returncode, result.stderr)
        self.assertTrue((self.root / "user-runner").is_dir())
        self.assertFalse((self.root / "xdg-config/systemd/user").exists())
        self.assertIn("Fertig", result.stderr)
        self.assertIn("kein Registrierungs-Token erforderlich oder abgerufen", result.stderr)
        self.assertNotIn("DRY-RUN-NO-TOKEN", result.stdout + result.stderr)
        self.assertNotIn(
            "registration-token",
            self.calls_file.read_text(encoding="utf-8"),
        )

    def test_public_repository_refuses_real_install_before_side_effects(self):
        result = self.run_installer(
            "--user",
            "--repo",
            "example/public-repository",
            PULSELIBRE_RUNNER_ROOT=self.root / "must-not-exist",
            STUB_VISIBILITY="public",
        )

        self.assertNotEqual(0, result.returncode)
        self.assertIn("ÖFFENTLICH", result.stderr)
        self.assertFalse((self.root / "must-not-exist").exists())
        calls = self.calls_file.read_text(encoding="utf-8")
        self.assertIn("gh api repos/example/public-repository", calls)
        self.assertNotIn("curl ", calls)

    def test_public_repository_guard_also_applies_to_dry_run(self):
        result = self.run_installer(
            "--system",
            "--dry-run",
            "--runner-version",
            "2.338.2",
            STUB_VISIBILITY="public",
        )

        self.assertNotEqual(0, result.returncode)
        self.assertIn("ÖFFENTLICH", result.stderr)
        calls = self.calls_file.read_text(encoding="utf-8")
        self.assertIn("gh api repos/jochenkunkel/pulselibre", calls)
        self.assertNotIn("registration-token", calls)
        self.assertNotIn("curl ", calls)
        self.assertNotIn("mkdir ", calls)


if __name__ == "__main__":
    unittest.main()
