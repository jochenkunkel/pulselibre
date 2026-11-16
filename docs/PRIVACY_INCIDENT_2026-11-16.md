# Critical privacy incident — 2026-11-16

## Confirmed exposure
A user-supplied CIRQA overnight JSONL recording was tracked in a public GitHub repository at `data/overnight/pulselibre-cirqa-overnight.jsonl`. It contains timestamps and individual health metrics (including heart rate, SpO2, RR intervals, stress and activity). The document `docs/CIRQA_OVERNIGHT_FIRST_RESULTS.md` contained derived precise measurements and time ranges. These data must be treated as potentially identifiable health data.

## Completed containment on main
- Commit `7d9d6a5`: delete recording from current tree.
- Commit `928c759`: redact precise metrics from current report.
- Commit `e71e842`: ignore common telemetry, databases, exports and private keys.
- Commit `65e2fda`: add guarded local history-rewrite script.

## NOT YET REMEDIATED — immediate repository owner action
1. **Change repository visibility to PRIVATE immediately.** GitHub repository Settings → General → Danger Zone → Change repository visibility.
2. Run `scripts/purge-sensitive-history.sh --i-understand-history-rewrite` only from a **disposable mirror clone** after reviewing its effects and coordinating collaborators. The script rewrites all refs locally; a deliberate force mirror push is required. It removes both historical recording and the complete historical derived report. Never run it against the only copy.
3. Coordinate with contributors to discard stale clones and reclone. Inspect forks, tags, pull-request refs, releases, artifacts, issue attachments and caches. Contact GitHub Support for sensitive-data removal from cached views / inaccessible refs as applicable.
4. Check GitHub Actions artifacts and logs for uploaded health telemetry and remove affected artifacts; this was **not verified**.
5. Conduct a full historical secret scan and review any other personal details in docs before restoring public visibility.
6. Consider whether GDPR incident notification duties apply based on exposure, identifiability, risk, access, controller role and timing. Seek qualified privacy/legal advice when necessary.

## Limits
Deleting files from `main` does **not** delete Git history or third-party copies. The GitHub connector used for the initial remediation cannot change visibility, force-push rewritten history, purge GitHub caches, or inspect every external clone. No claim of complete erasure is justified.
