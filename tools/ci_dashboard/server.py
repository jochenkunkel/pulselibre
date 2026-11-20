#!/usr/bin/env python3
"""Local, read-only GitHub Actions status dashboard."""

from __future__ import annotations

import argparse
import json
import re
import subprocess
import threading
import time
from concurrent.futures import ThreadPoolExecutor
from datetime import datetime, timezone
from http.server import BaseHTTPRequestHandler, ThreadingHTTPServer
from urllib.parse import urlsplit


DEFAULT_REPO = "jochenkunkel/pulselibre"
DEFAULT_RUN_LIMIT = 25
JOB_DETAIL_LIMIT = 8
CACHE_SECONDS = 12
ACTIVE_STATUSES = {"queued", "in_progress", "waiting", "requested", "pending"}
REPO_PATTERN = re.compile(r"^[A-Za-z0-9_.-]+/[A-Za-z0-9_.-]+$")


class GitHubCli:
    """Use the user's existing `gh` authentication; never accept/store tokens."""

    def __init__(self, repo: str) -> None:
        if not REPO_PATTERN.fullmatch(repo):
            raise ValueError("Repository must have the form OWNER/REPO")
        self.repo = repo

    def api(self, endpoint: str) -> dict:
        result = subprocess.run(
            ["gh", "api", "--hostname", "github.com", endpoint],
            check=False,
            capture_output=True,
            text=True,
            timeout=20,
        )
        if result.returncode:
            raise RuntimeError(
                "GitHub CLI request failed. Check network access and run "
                "`gh auth status` in a terminal."
            )
        try:
            value = json.loads(result.stdout)
        except json.JSONDecodeError as exc:
            raise RuntimeError("GitHub returned an unreadable API response.") from exc
        if not isinstance(value, dict):
            raise RuntimeError("GitHub returned an unexpected API response.")
        return value

    def runs(self, limit: int) -> list[dict]:
        payload = self.api(
            f"repos/{self.repo}/actions/runs?per_page={limit}"
        )
        runs = payload.get("workflow_runs", [])
        return runs if isinstance(runs, list) else []

    def run_jobs(self, run_id: int) -> list[dict]:
        payload = self.api(
            f"repos/{self.repo}/actions/runs/{run_id}/jobs?per_page=100"
        )
        jobs = payload.get("jobs", [])
        return jobs if isinstance(jobs, list) else []

    def runners(self) -> list[dict]:
        payload = self.api(f"repos/{self.repo}/actions/runners?per_page=100")
        runners = payload.get("runners", [])
        return runners if isinstance(runners, list) else []


def _current_step(job: dict) -> str | None:
    for step in job.get("steps") or []:
        if step.get("status") == "in_progress":
            return step.get("name")
    return None


def normalize_job(job: dict) -> dict:
    return {
        "name": job.get("name") or "Unnamed job",
        "status": job.get("status") or "unknown",
        "conclusion": job.get("conclusion"),
        "runner_name": job.get("runner_name"),
        "runner_id": job.get("runner_id"),
        "current_step": _current_step(job),
        "started_at": job.get("started_at"),
        "completed_at": job.get("completed_at"),
        "html_url": job.get("html_url"),
    }


def normalize_run(run: dict, jobs: list[dict] | None = None) -> dict:
    return {
        "id": run.get("id"),
        "name": run.get("name") or "GitHub Actions",
        "display_title": run.get("display_title") or run.get("name") or "Workflow run",
        "event": run.get("event"),
        "branch": run.get("head_branch"),
        "head_sha": (run.get("head_sha") or "")[:12],
        "status": run.get("status") or "unknown",
        "conclusion": run.get("conclusion"),
        "created_at": run.get("created_at"),
        "run_started_at": run.get("run_started_at"),
        "updated_at": run.get("updated_at"),
        "html_url": run.get("html_url"),
        "jobs": [normalize_job(job) for job in (jobs or [])],
    }


class Dashboard:
    def __init__(self, repo: str, run_limit: int) -> None:
        self.github = GitHubCli(repo)
        self.repo = repo
        self.run_limit = run_limit
        self._cache: tuple[float, dict] | None = None
        self._lock = threading.Lock()

    def snapshot(self, force: bool = False) -> dict:
        with self._lock:
            now = time.monotonic()
            if (
                not force
                and self._cache
                and now - self._cache[0] < CACHE_SECONDS
            ):
                return self._cache[1]

            runs = self.github.runs(self.run_limit)
            runner_future = None
            with ThreadPoolExecutor(max_workers=9) as pool:
                runner_future = pool.submit(self.github.runners)
                job_runs = [
                    run
                    for index, run in enumerate(runs)
                    if run.get("status") in ACTIVE_STATUSES
                    or index < JOB_DETAIL_LIMIT
                ]
                jobs_by_run: dict[int, list[dict]] = {}
                futures = {
                    pool.submit(self.github.run_jobs, int(run["id"])): int(run["id"])
                    for run in job_runs
                    if run.get("id") is not None
                }
                for future, run_id in futures.items():
                    try:
                        jobs_by_run[run_id] = future.result()
                    except (RuntimeError, subprocess.SubprocessError, ValueError):
                        jobs_by_run[run_id] = []
                try:
                    runners = runner_future.result()
                    runner_error = None
                except (RuntimeError, subprocess.SubprocessError, ValueError):
                    runners = []
                    runner_error = (
                        "Could not read runner status. Check repository permissions."
                    )

            normalized_runs = [
                normalize_run(run, jobs_by_run.get(int(run["id"]), []))
                if run.get("id") is not None
                else normalize_run(run)
                for run in runs
            ]
            result = {
                "repo": self.repo,
                "loaded_at": datetime.now(timezone.utc).isoformat(),
                "runs": normalized_runs,
                "runners": [
                    {
                        "name": runner.get("name"),
                        "status": runner.get("status"),
                        "busy": runner.get("busy"),
                        "labels": [
                            label.get("name")
                            for label in runner.get("labels") or []
                            if isinstance(label, dict)
                        ],
                    }
                    for runner in runners
                ],
                "runner_error": runner_error,
                "job_details_note": (
                    f"Job details shown for active runs and the latest "
                    f"{JOB_DETAIL_LIMIT} historical runs."
                ),
            }
            self._cache = (time.monotonic(), result)
            return result


PAGE = r"""<!doctype html>
<html lang="en">
<head>
  <meta charset="utf-8">
  <meta name="viewport" content="width=device-width,initial-scale=1">
  <meta http-equiv="Content-Security-Policy"
        content="default-src 'self'; style-src 'unsafe-inline'; script-src 'unsafe-inline'; connect-src 'self';">
  <title>Local CI status</title>
  <style>
    :root { color-scheme: dark; font: 15px/1.5 system-ui,sans-serif; background:#10141d;color:#e7ebf2 }
    body { max-width:1200px;margin:0 auto;padding:24px }
    header,.toolbar,.runner-list,.job { display:flex;align-items:center;gap:12px;flex-wrap:wrap }
    header { justify-content:space-between }
    h1 { font-size:1.6rem;margin:0 } h2 { font-size:1.15rem;margin:24px 0 10px }
    .muted,small { color:#a9b3c5 } button { background:#29354a;color:#fff;border:1px solid #45536d;border-radius:7px;padding:8px 12px;cursor:pointer }
    .card { background:#171e2b;border:1px solid #2b3547;border-radius:10px;padding:14px;margin:10px 0 }
    .run-head { display:flex;justify-content:space-between;gap:12px;align-items:flex-start }
    .run-title { font-weight:650 } a { color:#8db8ff;text-decoration:none } a:hover { text-decoration:underline }
    .pill { border-radius:999px;padding:2px 9px;font-size:.82rem;background:#30394a }
    .success { color:#83e6a1;background:#153a2a } .failure,.cancelled,.timed_out,.action_required { color:#ff9d9d;background:#49252b }
    .in_progress,queued,waiting,requested,pending { color:#ffdc84;background:#493e20 }
    .neutral { color:#bbc4d3;background:#30394a } .job { border-top:1px solid #2b3547;margin-top:10px;padding-top:8px;justify-content:space-between }
    .runner { background:#171e2b;border-radius:8px;padding:8px 12px }
    .error { color:#ffadad;background:#49252b;padding:12px;border-radius:8px }
    @media(max-width:600px){body{padding:14px}.run-head{display:block}.run-head .pill{display:inline-block;margin-top:6px}}
  </style>
</head>
<body>
  <header><div><h1>Local CI status</h1><div id="repo" class="muted">Loading…</div></div>
    <div class="toolbar"><button id="refresh">Refresh now</button><span class="muted">auto-refresh: 15s</span></div>
  </header>
  <div id="error"></div>
  <h2>Runner pool</h2><div id="runners" class="runner-list"></div>
  <h2>Running / queued</h2><div id="active"></div>
  <h2>Recent history</h2><div id="history"></div>
  <footer id="updated" class="muted"></footer>
<script>
const esc = (s) => String(s ?? "").replace(/[&<>"']/g, c => ({"&":"&amp;","<":"&lt;",">":"&gt;","\"":"&quot;","'":"&#39;"}[c]));
const stamp = (s) => s ? new Date(s).toLocaleString() : "—";
const githubUrl = (s) => { try { const u = new URL(s); return u.origin === "https://github.com" ? u.href : "#"; } catch { return "#"; } };
function statusPill(status, conclusion) {
  const label = conclusion || status || "unknown";
  const cls = ["success","failure","cancelled","timed_out","in_progress","queued","waiting","requested","pending"].includes(label) ? label : "neutral";
  return `<span class="pill ${cls}">${esc(label.replaceAll("_"," "))}</span>`;
}
function renderRun(run) {
  const jobHtml = (run.jobs || []).map(job => `<div class="job">
    <span><strong>${esc(job.name)}</strong>${job.current_step ? ` · ${esc(job.current_step)}` : ""}<br>
    <small>${job.runner_name ? `Runner: ${esc(job.runner_name)} (#${esc(job.runner_id)})` : "Runner not assigned"} · ${esc(job.status)}</small></span>
    ${statusPill(job.status,job.conclusion)}
  </div>`).join("");
  return `<article class="card"><div class="run-head">
    <span><span class="run-title">${esc(run.display_title)}</span><br>
    <small>${esc(run.event)} · ${esc(run.branch)} · ${esc(run.head_sha)} · ${stamp(run.created_at)}</small></span>
    <span>${statusPill(run.status,run.conclusion)} <a href="${esc(githubUrl(run.html_url))}" target="_blank" rel="noreferrer">Open run ↗</a></span>
  </div>${jobHtml || '<div class="muted" style="margin-top:8px">No job details fetched for this historical run.</div>'}</article>`;
}
async function refresh(force=false) {
  const error = document.getElementById("error");
  error.innerHTML = "";
  try {
    const response = await fetch(`/api/status${force ? "?refresh=1" : ""}`, {cache:"no-store"});
    const data = await response.json();
    if (!response.ok) throw new Error(data.error || `HTTP ${response.status}`);
    document.getElementById("repo").textContent = data.repo;
    document.getElementById("runners").innerHTML = data.runners.length
      ? data.runners.map(r => `<span class="runner">${esc(r.name)} · ${esc(r.status)} · ${r.busy ? "busy" : "idle"} · ${esc((r.labels||[]).join(", "))}</span>`).join("")
      : '<span class="muted">No runner data returned.</span>';
    if (data.runner_error) error.innerHTML = `<p class="error">${esc(data.runner_error)}</p>`;
    const active = data.runs.filter(r => ["queued","in_progress","waiting","requested","pending"].includes(r.status));
    const history = data.runs.filter(r => !["queued","in_progress","waiting","requested","pending"].includes(r.status));
    document.getElementById("active").innerHTML = active.length ? active.map(renderRun).join("") : '<div class="muted">No active or queued workflow runs.</div>';
    document.getElementById("history").innerHTML = history.length ? history.map(renderRun).join("") : '<div class="muted">No recent runs.</div>';
    document.getElementById("updated").textContent = `Updated ${stamp(data.loaded_at)}. ${data.job_details_note}`;
  } catch (e) {
    error.innerHTML = `<p class="error">${esc(e.message)} Check that \`gh auth status\` succeeds.</p>`;
  }
}
document.getElementById("refresh").addEventListener("click", () => refresh(true));
refresh(); setInterval(() => refresh(), 15000);
</script>
</body>
</html>
"""


def make_handler(dashboard: Dashboard):
    class Handler(BaseHTTPRequestHandler):
        server_version = "LocalCiDashboard/1.0"

        def _send(self, status: int, body: bytes, content_type: str) -> None:
            self.send_response(status)
            self.send_header("Content-Type", content_type)
            self.send_header("Content-Length", str(len(body)))
            self.send_header("Cache-Control", "no-store")
            self.send_header("X-Content-Type-Options", "nosniff")
            self.end_headers()
            self.wfile.write(body)

        def do_GET(self) -> None:
            path = urlsplit(self.path).path
            if path == "/":
                self._send(200, PAGE.encode("utf-8"), "text/html; charset=utf-8")
                return
            if path == "/health":
                self._send(200, b'{"status":"ok"}', "application/json")
                return
            if path == "/api/status":
                force = "refresh=1" in urlsplit(self.path).query.split("&")
                try:
                    payload = dashboard.snapshot(force=force)
                    body = json.dumps(payload).encode("utf-8")
                    self._send(200, body, "application/json; charset=utf-8")
                except (RuntimeError, subprocess.SubprocessError, OSError) as exc:
                    body = json.dumps({"error": str(exc)}).encode("utf-8")
                    self._send(502, body, "application/json; charset=utf-8")
                return
            self._send(404, b"Not found", "text/plain; charset=utf-8")

        def do_POST(self) -> None:
            self._send(405, b"Read-only dashboard", "text/plain; charset=utf-8")

        def log_message(self, format: str, *args) -> None:
            # Keep the terminal useful without dumping query payloads or credentials.
            print(f"{self.log_date_time_string()} {self.client_address[0]} {args[0]}")

    return Handler


def main() -> None:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--repo", default=DEFAULT_REPO, help="GitHub OWNER/REPO")
    parser.add_argument("--host", default="127.0.0.1", help="Loopback address only")
    parser.add_argument("--port", type=int, default=8765)
    parser.add_argument("--runs", type=int, default=DEFAULT_RUN_LIMIT)
    args = parser.parse_args()

    if args.host not in {"127.0.0.1", "localhost", "::1"}:
        parser.error("--host must be a loopback address; remote binding is disabled")
    if not 1 <= args.port <= 65535:
        parser.error("--port must be between 1 and 65535")
    if not 1 <= args.runs <= 50:
        parser.error("--runs must be between 1 and 50")

    dashboard = Dashboard(args.repo, args.runs)
    server = ThreadingHTTPServer((args.host, args.port), make_handler(dashboard))
    print(f"CI dashboard for {args.repo}: http://{args.host}:{args.port}")
    print("Read-only; stop with Ctrl+C.")
    try:
        server.serve_forever()
    except KeyboardInterrupt:
        print("\nStopping dashboard.")
    finally:
        server.server_close()


if __name__ == "__main__":
    main()
