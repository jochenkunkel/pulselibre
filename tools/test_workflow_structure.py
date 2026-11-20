"""Strukturtests für die GitHub-Actions-Workflows.

Diese Tests sichern die Fehler ab, die im hybriden CI-Umbau tatsächlich
aufgetreten sind oder die Sicherheitsregeln verletzen würden:

* ``fromJSON('ubuntu-latest')`` ist kein gültiges JSON. GitHub verwirft den
  Job dann stillschweigend (``Error when evaluating 'runs-on'``). Der
  ``prepare``-Job muss deshalb immer ein JSON-Array liefern.
* GitHub wertet ``needs`` in Job-Namen nicht aus; dort stehen sonst wörtlich
  ``${{ ... }}`` in der Oberfläche.
* Alle Actions müssen auf einen vollständigen Commit-SHA gepinnt sein.
* Pull Requests dürfen niemals auf dem Self-hosted Runner landen.

Die Tests lesen nur Dateien und benötigen kein Netzwerk.
"""

from __future__ import annotations

import pathlib
import re
import unittest

import yaml

REPO_ROOT = pathlib.Path(__file__).resolve().parents[1]
WORKFLOW_DIR = REPO_ROOT / ".github" / "workflows"
ANDROID_WORKFLOW = WORKFLOW_DIR / "android.yml"

SHA_PINNED_ACTION = re.compile(r"^[^@\s]+@[0-9a-f]{40}$")
LOCAL_RUNNER_JSON = '["self-hosted","linux","x64","pulselibre"]'
CLOUD_RUNNER_JSON = '["ubuntu-latest"]'


def load_workflow(path: pathlib.Path) -> dict:
    with path.open(encoding="utf-8") as handle:
        return yaml.safe_load(handle)


def workflow_triggers(workflow: dict) -> dict:
    # PyYAML löst ``on:`` nach YAML 1.1 als Boolean True auf.
    triggers = workflow.get("on", workflow.get(True))
    if not isinstance(triggers, dict):
        raise AssertionError("Workflow hat keinen auswertbaren 'on'-Block")
    return triggers


def iter_steps(workflow: dict):
    for job_id, job in workflow["jobs"].items():
        for step in job.get("steps", []):
            yield job_id, step


class AndroidWorkflowTest(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        cls.workflow = load_workflow(ANDROID_WORKFLOW)

    def test_manual_inputs_are_independent(self):
        inputs = workflow_triggers(self.workflow)["workflow_dispatch"]["inputs"]

        environment = inputs["run_environment"]
        self.assertEqual(environment["type"], "choice")
        self.assertEqual(environment["options"], ["LOCAL", "CLOUD"])
        self.assertIn(environment["default"], environment["options"])

        scope = inputs["test_scope"]
        self.assertEqual(scope["type"], "choice")
        self.assertEqual(scope["options"], ["QUICK", "FULL"])
        self.assertIn(scope["default"], scope["options"])

    def test_local_routing_does_not_depend_on_hosted_prepare_job(self):
        # A hosted prepare job would itself be blocked by a billing lock before
        # GitHub could route the actual Android work to the self-hosted runner.
        self.assertNotIn("prepare", self.workflow["jobs"])
        for job_id in ("build", "emulator"):
            with self.subTest(job=job_id):
                job = self.workflow["jobs"][job_id]
                self.assertNotIn("prepare", job.get("needs", []))
                self.assertIn("fromJSON", job["runs-on"])
                self.assertIn(LOCAL_RUNNER_JSON, job["runs-on"])
                self.assertIn(CLOUD_RUNNER_JSON, job["runs-on"])

    def test_direct_local_routing_preserves_event_and_pr_safety_guards(self):
        for job_id in ("build", "emulator"):
            with self.subTest(job=job_id):
                routing = self.workflow["jobs"][job_id]["runs-on"]
                self.assertIn("github.event_name == 'push'", routing)
                self.assertIn("github.event_name == 'workflow_dispatch'", routing)
                self.assertIn("inputs.run_environment == 'LOCAL'", routing)
                self.assertIn("vars.PULSELIBRE_LOCAL_RUNNER_ENABLED == 'true'", routing)
                self.assertIn("vars.PULSELIBRE_PR_ON_LOCAL_ENABLED == 'true'", routing)
                self.assertIn("github.event.pull_request.head.repo.fork == false", routing)

    def test_scope_is_derived_without_prepare_outputs(self):
        build = self.workflow["jobs"]["build"]
        emulator = self.workflow["jobs"]["emulator"]
        self.assertIn("inputs.test_scope", build["env"]["CI_SCOPE"])
        self.assertIn("inputs.test_scope", emulator["env"]["CI_SCOPE"])
        self.assertNotIn("needs.prepare", str(self.workflow))

    def test_job_names_are_static(self):
        # GitHub wertet 'needs' in Job-Namen nicht aus; Expressions erscheinen
        # sonst wörtlich in der Oberfläche.
        for job_id, job in self.workflow["jobs"].items():
            with self.subTest(job=job_id):
                self.assertNotIn("${{", job.get("name", ""))

    def test_emulator_only_runs_for_full_scope(self):
        emulator = self.workflow["jobs"]["emulator"]
        self.assertIn("inputs.test_scope == 'FULL'", emulator["if"])
        self.assertIn("github.event_name != 'workflow_dispatch'", emulator["if"])

    def test_concurrency_cancels_only_superseded_pull_requests(self):
        concurrency = self.workflow["concurrency"]
        self.assertEqual(
            concurrency["cancel-in-progress"],
            "${{ github.event_name == 'pull_request' }}",
        )
        self.assertIn("github.ref", concurrency["group"])

    def test_checkout_never_persists_credentials(self):
        checkouts = [
            step
            for _, step in iter_steps(self.workflow)
            if str(step.get("uses", "")).startswith("actions/checkout@")
        ]
        self.assertTrue(checkouts)
        for step in checkouts:
            with self.subTest(step=step.get("name", "checkout")):
                self.assertFalse(step.get("with", {}).get("persist-credentials", True))

    def test_no_write_all_permissions(self):
        self.assertNotEqual(self.workflow.get("permissions"), "write-all")
        for job_id, job in self.workflow["jobs"].items():
            with self.subTest(job=job_id):
                self.assertNotEqual(job.get("permissions"), "write-all")


class AllWorkflowsTest(unittest.TestCase):
    def test_all_actions_are_pinned_to_commit_sha(self):
        failures = []
        for path in sorted(WORKFLOW_DIR.glob("*.yml")):
            workflow = load_workflow(path)
            for job_id, step in iter_steps(workflow):
                uses = str(step.get("uses", ""))
                if not uses or uses.startswith("./") or uses.startswith("docker://"):
                    continue
                if not SHA_PINNED_ACTION.match(uses):
                    failures.append(f"{path.name}:{job_id}: {uses}")
        self.assertEqual(failures, [], "Actions ohne vollständigen SHA-Pin gefunden")

    def test_workflows_parse_as_mapping(self):
        for path in sorted(WORKFLOW_DIR.glob("*.yml")):
            with self.subTest(workflow=path.name):
                workflow = load_workflow(path)
                self.assertIsInstance(workflow, dict)
                self.assertIn("jobs", workflow)
                self.assertTrue(workflow["jobs"])


if __name__ == "__main__":
    unittest.main()
