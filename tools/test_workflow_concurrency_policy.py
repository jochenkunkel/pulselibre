"""Deterministic offline tests for the Android workflow concurrency policy.

This small scheduler model encodes the GitHub concurrency behavior relied on by
the workflow: one running + one pending run per group, pending runs coalesce to
the newest, and only PR submissions may cancel the active run. The companion
workflow-structure test binds the configured GitHub expression to this policy.
It does not claim to emulate GitHub Actions itself.
"""

from dataclasses import dataclass
import unittest


@dataclass(frozen=True)
class Run:
    run_id: str
    event: str
    ref: str
    environment: str = "auto"

    @property
    def group(self):
        return self.ref, self.environment


class ConcurrencyPolicyModel:
    def __init__(self):
        self.running = {}
        self.pending = {}
        self.cancelled = []
        self.superseded_pending = []

    @staticmethod
    def cancel_in_progress(run):
        return run.event == "pull_request"

    def submit(self, run):
        group = run.group
        if group not in self.running:
            self.running[group] = run
            return

        if self.cancel_in_progress(run):
            self.cancelled.append(self.running[group])
            self.running[group] = run
            old_pending = self.pending.pop(group, None)
            if old_pending:
                self.superseded_pending.append(old_pending)
            return

        old_pending = self.pending.get(group)
        if old_pending:
            self.superseded_pending.append(old_pending)
        self.pending[group] = run

    def complete(self, group):
        finished = self.running.pop(group)
        next_run = self.pending.pop(group, None)
        if next_run:
            self.running[group] = next_run
        return finished


class WorkflowConcurrencyPolicyTests(unittest.TestCase):
    def test_main_run_finishes_and_only_latest_pending_main_runs_next(self):
        policy = ConcurrencyPolicyModel()
        group = ("refs/heads/main", "auto")
        policy.submit(Run("main-a", "push", *group))
        policy.submit(Run("main-b", "push", *group))
        policy.submit(Run("main-c", "push", *group))

        self.assertEqual(policy.running[group].run_id, "main-a")
        self.assertEqual(policy.pending[group].run_id, "main-c")
        self.assertEqual([run.run_id for run in policy.cancelled], [])
        self.assertEqual(
            [run.run_id for run in policy.superseded_pending], ["main-b"]
        )

        policy.complete(group)
        self.assertEqual(policy.running[group].run_id, "main-c")
        self.assertNotIn(group, policy.pending)

    def test_new_pull_request_run_cancels_only_same_pr_active_run(self):
        policy = ConcurrencyPolicyModel()
        group = ("refs/pull/42/merge", "auto")
        policy.submit(Run("pr-old", "pull_request", *group))
        policy.submit(Run("pr-new", "pull_request", *group))

        self.assertEqual([run.run_id for run in policy.cancelled], ["pr-old"])
        self.assertEqual(policy.running[group].run_id, "pr-new")

    def test_push_does_not_cancel_an_active_pull_request_run(self):
        policy = ConcurrencyPolicyModel()
        pr_group = ("refs/pull/42/merge", "auto")
        main_group = ("refs/heads/main", "auto")
        policy.submit(Run("pr-active", "pull_request", *pr_group))
        policy.submit(Run("main-push", "push", *main_group))

        self.assertEqual(policy.running[pr_group].run_id, "pr-active")
        self.assertEqual(policy.running[main_group].run_id, "main-push")
        self.assertEqual(policy.cancelled, [])

    def test_manual_runs_also_preserve_active_validation(self):
        policy = ConcurrencyPolicyModel()
        group = ("refs/heads/main", "CLOUD")
        policy.submit(Run("dispatch-active", "workflow_dispatch", *group))
        policy.submit(Run("dispatch-newer", "workflow_dispatch", *group))

        self.assertEqual(policy.running[group].run_id, "dispatch-active")
        self.assertEqual(policy.pending[group].run_id, "dispatch-newer")
        self.assertEqual(policy.cancelled, [])


if __name__ == "__main__":
    unittest.main()
