import unittest

try:
    from .server import Dashboard, normalize_job, normalize_run
except ImportError:
    from server import Dashboard, normalize_job, normalize_run


class FakeGitHub:
    def runs(self, limit):
        return [
            {
                "id": 7,
                "name": "Android CI",
                "status": "in_progress",
                "head_branch": "main",
                "head_sha": "123456789abc",
            },
            {
                "id": 6,
                "name": "Android CI",
                "status": "completed",
                "conclusion": "success",
            },
        ]

    def run_jobs(self, run_id):
        if run_id == 7:
            return [
                {
                    "name": "Build",
                    "status": "in_progress",
                    "runner_name": "test-runner",
                    "runner_id": 123,
                    "steps": [{"name": "Gradle", "status": "in_progress"}],
                }
            ]
        return []

    def runners(self):
        return [
            {
                "name": "test-runner",
                "status": "online",
                "busy": True,
                "labels": [{"name": "pulselibre"}],
            }
        ]


class DashboardFormattingTests(unittest.TestCase):
    def test_snapshot_includes_live_jobs_and_runner_state(self):
        dashboard = Dashboard("owner/repo", 10)
        dashboard.github = FakeGitHub()

        data = dashboard.snapshot()

        self.assertEqual(data["runs"][0]["jobs"][0]["runner_name"], "test-runner")
        self.assertEqual(data["runs"][0]["jobs"][0]["current_step"], "Gradle")
        self.assertEqual(data["runners"][0]["status"], "online")
        self.assertTrue(data["runners"][0]["busy"])
        self.assertEqual(data["runs"][1]["conclusion"], "success")

    def test_normalizes_run_and_truncates_sha(self):
        run = normalize_run(
            {
                "id": 123,
                "name": "Android CI",
                "display_title": "Android CI · main · AUTO · FULL",
                "event": "push",
                "head_branch": "main",
                "head_sha": "0123456789abcdef",
                "status": "completed",
                "conclusion": "success",
                "html_url": "https://github.test/run/123",
            }
        )

        self.assertEqual(run["id"], 123)
        self.assertEqual(run["head_sha"], "0123456789ab")
        self.assertEqual(run["conclusion"], "success")
        self.assertEqual(run["jobs"], [])

    def test_normalizes_job_with_runner_and_current_step(self):
        job = normalize_job(
            {
                "name": "Emulator tests",
                "status": "in_progress",
                "runner_name": "test-runner",
                "runner_id": 123,
                "steps": [
                    {"name": "Set up job", "status": "completed"},
                    {"name": "Instrumentation", "status": "in_progress"},
                ],
            }
        )

        self.assertEqual(job["runner_name"], "test-runner")
        self.assertEqual(job["runner_id"], 123)
        self.assertEqual(job["current_step"], "Instrumentation")

    def test_missing_job_fields_are_safe(self):
        job = normalize_job({})
        self.assertEqual(job["name"], "Unnamed job")
        self.assertEqual(job["status"], "unknown")
        self.assertIsNone(job["current_step"])


if __name__ == "__main__":
    unittest.main()
