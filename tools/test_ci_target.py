"""Regression tests for the hybrid CI runner/scope resolution (issue: hybrid CI).

The decision logic lives in tools/ci/resolve-target.sh and is pure: these tests
pin the safety rules so a future edit cannot silently route untrusted pull
request code to the self-hosted runner.
"""
import os
import pathlib
import subprocess
import unittest

ROOT = pathlib.Path(__file__).resolve().parents[1]
RESOLVER = ROOT / "tools" / "ci" / "resolve-target.sh"

LOCAL_LABELS = '["self-hosted","linux","x64","pulselibre"]'
CLOUD_LABEL = "ubuntu-latest"


class ResolveTargetTests(unittest.TestCase):
    def resolve(self, **env_overrides):
        env = {
            "PATH": os.environ.get("PATH", "/usr/bin:/bin"),
            "HOME": os.environ.get("HOME", "/tmp"),
        }
        env.update(env_overrides)
        result = subprocess.run(
            ["bash", str(RESOLVER)],
            env=env,
            capture_output=True,
            text=True,
            timeout=20,
        )
        self.assertEqual(0, result.returncode, result.stderr)
        return dict(
            line.split("=", 1) for line in result.stdout.splitlines() if "=" in line
        )

    def test_manual_local_quick(self):
        out = self.resolve(
            EVENT_NAME="workflow_dispatch",
            REQUESTED_ENVIRONMENT="LOCAL",
            REQUESTED_SCOPE="QUICK",
            LOCAL_RUNNER_ENABLED="true",
        )
        self.assertEqual("LOCAL", out["environment"])
        self.assertEqual("QUICK", out["scope"])
        self.assertEqual(LOCAL_LABELS, out["runs_on"])

    def test_manual_cloud_full(self):
        out = self.resolve(
            EVENT_NAME="workflow_dispatch",
            REQUESTED_ENVIRONMENT="CLOUD",
            REQUESTED_SCOPE="FULL",
            LOCAL_RUNNER_ENABLED="true",
        )
        self.assertEqual("CLOUD", out["environment"])
        self.assertEqual("FULL", out["scope"])
        self.assertEqual(CLOUD_LABEL, out["runs_on"])

    def test_manual_local_without_enabled_runner_falls_back_to_cloud(self):
        out = self.resolve(
            EVENT_NAME="workflow_dispatch",
            REQUESTED_ENVIRONMENT="LOCAL",
            REQUESTED_SCOPE="QUICK",
            LOCAL_RUNNER_ENABLED="false",
        )
        self.assertEqual("CLOUD", out["environment"])
        self.assertEqual(CLOUD_LABEL, out["runs_on"])
        self.assertIn("Fallback", out["reason"])

    def test_manual_local_with_unset_variable_falls_back_to_cloud(self):
        out = self.resolve(
            EVENT_NAME="workflow_dispatch",
            REQUESTED_ENVIRONMENT="LOCAL",
            REQUESTED_SCOPE="QUICK",
        )
        self.assertEqual("CLOUD", out["environment"])

    def test_push_to_main_defaults_to_local_full(self):
        out = self.resolve(EVENT_NAME="push", LOCAL_RUNNER_ENABLED="true")
        self.assertEqual("LOCAL", out["environment"])
        self.assertEqual("FULL", out["scope"])
        self.assertEqual(LOCAL_LABELS, out["runs_on"])

    def test_push_to_main_without_runner_uses_cloud(self):
        out = self.resolve(EVENT_NAME="push")
        self.assertEqual("CLOUD", out["environment"])
        self.assertEqual(CLOUD_LABEL, out["runs_on"])

    def test_pull_request_never_local_without_explicit_pr_opt_in(self):
        out = self.resolve(
            EVENT_NAME="pull_request",
            LOCAL_RUNNER_ENABLED="true",
            PR_ON_LOCAL_ENABLED="false",
        )
        self.assertEqual("CLOUD", out["environment"])
        self.assertEqual(CLOUD_LABEL, out["runs_on"])
        self.assertEqual("FULL", out["scope"])

    def test_pull_request_from_fork_is_always_cloud(self):
        out = self.resolve(
            EVENT_NAME="pull_request",
            LOCAL_RUNNER_ENABLED="true",
            PR_ON_LOCAL_ENABLED="true",
            PR_FROM_FORK="true",
        )
        self.assertEqual("CLOUD", out["environment"])

    def test_pull_request_with_explicit_opt_in_is_local(self):
        out = self.resolve(
            EVENT_NAME="pull_request",
            LOCAL_RUNNER_ENABLED="true",
            PR_ON_LOCAL_ENABLED="true",
            PR_FROM_FORK="false",
        )
        self.assertEqual("LOCAL", out["environment"])

    def test_unknown_trigger_never_uses_local(self):
        out = self.resolve(EVENT_NAME="schedule", LOCAL_RUNNER_ENABLED="true")
        self.assertEqual("CLOUD", out["environment"])
        self.assertEqual(CLOUD_LABEL, out["runs_on"])

    def test_invalid_manual_environment_is_cloud(self):
        out = self.resolve(
            EVENT_NAME="workflow_dispatch",
            REQUESTED_ENVIRONMENT="SOMEWHERE",
            REQUESTED_SCOPE="QUICK",
            LOCAL_RUNNER_ENABLED="true",
        )
        self.assertEqual("CLOUD", out["environment"])
        self.assertIn("Ungültiger", out["reason"])

    def test_runs_on_is_always_valid(self):
        cases = [
            {"EVENT_NAME": "push"},
            {"EVENT_NAME": "pull_request"},
            {"EVENT_NAME": "workflow_dispatch", "REQUESTED_ENVIRONMENT": "LOCAL"},
            {"EVENT_NAME": "workflow_dispatch", "REQUESTED_ENVIRONMENT": "CLOUD"},
            {"EVENT_NAME": "", "LOCAL_RUNNER_ENABLED": "true"},
        ]
        for case in cases:
            with self.subTest(case=case):
                out = self.resolve(**case)
                self.assertIn(out["runs_on"], (LOCAL_LABELS, CLOUD_LABEL))
                self.assertIn(out["environment"], ("LOCAL", "CLOUD"))
                self.assertIn(out["scope"], ("QUICK", "FULL"))

    def test_scope_values_are_normalized(self):
        out = self.resolve(
            EVENT_NAME="workflow_dispatch",
            REQUESTED_ENVIRONMENT="cloud",
            REQUESTED_SCOPE="full",
            LOCAL_RUNNER_ENABLED="TRUE",
        )
        self.assertEqual("CLOUD", out["environment"])
        self.assertEqual("FULL", out["scope"])


if __name__ == "__main__":
    unittest.main()
