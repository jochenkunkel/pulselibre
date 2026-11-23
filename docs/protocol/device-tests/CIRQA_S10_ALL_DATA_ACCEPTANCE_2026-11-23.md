# S10 acceptance: all available band data in local SQLite (2026-11-23)

## Acceptance result

- Code fix: merged in PR #567 (`891bdcef`), followed by independent main commits
  `90d1b28b` and `28b49fb1`.
- Full CI on exact main head `28b49fb1`: [Android CI run 1989 / 45224433766](https://github.com/jochenkunkel/pulselibre/actions/runs/45224433766)
  completed `success` (Build/Unit/Lint/APK and API-35 emulator). Local FULL on the
  same head with `PULSELIBRE_VERSION_CODE=1989` also passed in 507 s: 990 JVM
  tests, 165 API-35 emulator tests, both new original-archive tests green,
  0 failures/errors.
- Release APK: versionCode 1989, signing certificate SHA-256
  `36e7f3b664fedb04f175a97a45079a845d781a1769db1a4b02b1c8bf51439177`, matching
  the certificate reported for the build the GUI agent installed on S10.
- S10: exactly one manually confirmed sync was executed in the FIT acceptance
  window. It ended **incomplete: 36/37 files**. There was no retry after that
  terminal result. Lease was then released (`FREI`).
- Important run-count caveat: Build 1989 also starts an automatic historical
  sync on app entry. The GUI agent reported an automatic 2/37 attempt during
  #529 before handing off the device; during this acceptance session a fresh
  36/37 status was visible after launching the app, before the manual run.
  Therefore the strict “one total request” condition was not met across app
  launches. The confirmed manual request remains exactly one; the app-entry
  auto-sync is an existing behavior and must be addressed before future
  controlled tests. No further request was sent.

## What the acceptance run must show

1. Before state: private product backup exported via the app and pulled only into
   a private host directory. The two backup files are mode 0600. No root,
   `run-as` or ADB access to app-private files is possible on this production
   build (`allowBackup=false`, not debuggable).
2. One manually confirmed historical download after the fix. Terminal UI status
   was observed; it was incomplete at 36/37 files. A probable app-entry
   automatic attempt also occurred, so the task's strict one-total-request
   constraint was not satisfied.
3. After state: second private backup export. Backup format 9 contains
   `downloaded_file_archive.jsonl` with complete original bytes, exact remote
   item references, whole-file SHA-256 checks and chunking below CursorWindow.
4. Offline comparison, aggregate only:
   - 38 unique originals / 642,709 bytes before and after; 0 new content
     versions, 0 removed versions, all stored-file hashes valid.
   - Remote reference associations increased 40 → 74 (+34).
   - Canonical observations remained 28,668; 0 added or removed rows.
   - All 34 FIT originals decoded (16,056 records). Across the rolling window
     2026-11-23 15:13:47–18:17 UTC, there were 0 new canonical observations,
     0 recent review candidates, and 0 decoded FIT files with recent candidates.
     Before and after coverage were both empty in that interval.
   - Existing FIT originals contained 109,960 occurrences of fields outside
     the supported profile mapping; their complete bytes remain recoverable
     from the SQLite originals archive.
5. No raw time series, identifiers, personal original bytes or screenshots enter
   Git. No reset, SetFlags, unpairing, manual retry or new BLE commands.

## Honesty boundary

This acceptance is **not a success for recent-history retrieval or full-history
coverage**. The freshness skip fix is in place, but the recent interval remained
absent and one of 37 listed files was not transferred. The remaining cause is
not established; transaction/filter context remains unverified as documented
in `CIRQA_RECENT_HISTORY_FRESHNESS_AUDIT_2026-11-23.md`. Do not run another
download until the app-entry automatic sync is disabled or deliberately used as
the single approved run, and the 36/37 outcome is understood.
