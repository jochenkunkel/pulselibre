# V27 bounded device result and status update
Von: OpenCode · An: alle · 2026-11-20, 07:54 UTC · Bezug: #149/#150/#144; [V27 plan](../../protocol/experiments/CIRQA_SYNC_V27_PLAN_2026-11-20.md)

## Bestätigte Fakten
- The single authorized V27 run completed after full Main CI `44694993958` passed on `f7a526fe62dc5e925f8053611873681a90df4c43`.
- Redacted result: one listing page, 25 entries (24 FIT, one non-FIT), all 25 transfers completed, zero failures, 488,067 aggregate bytes, no retry. The V25 key was left untouched; the separate V27 one-shot was consumed.
- The recorder was stopped before the run and restarted afterward.
- The post-run export passed ZIP/manifest/checksum checks; SQLite `quick_check` was `ok`; canonical-reference checks found no dangling references. Aggregate canonical observation and provenance row deltas are documented in the linked result report.
- Result report: `docs/protocol/device-tests/CIRQA_SYNC_V27_DEVICE_RESULT_2026-11-20.md`.

## Empfehlung / Bitte
- I am updating the current V27 status and linking the redacted result in `docs/PROJECT_STATUS.md`; please avoid overlapping edits to that status block until this branch is reviewed.
- The V27 experiment is complete, but #149, #150, and #144 remain open until their remaining acceptance criteria are met.
- Claude Code's active #155/#276 work and the #150 UI boundary are unchanged; no Test Laboratory, dashboard, or navigation code was modified.

## Unsicherheiten
- This single page does not establish complete device-history coverage. The accepted sync request may have marked buffered band data as transferred.
- A direct SQL `PRAGMA foreign_key_check` was unavailable through the production package; the post-backup reference audit and app `quick_check` are documented separately.

## Nicht anfassen
- No further BLE run, retry, V25-gate reset, destructive FileSync command, or unredacted data sharing.
- No parallel edits to Claude Code's active GUI/dashboard files or Test Laboratory D/E.
