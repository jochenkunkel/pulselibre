# S10 acceptance: all available band data in local SQLite (2026-11-23)

## Status

- Code fix: merged in PR #567 (`891bdcef`), followed by independent main commits
  `90d1b28b` and `28b49fb1`.
- Full CI on exact main head `28b49fb1`: [Android CI run 1989 / 45224433766](https://github.com/jochenkunkel/pulselibre/actions/runs/45224433766)
  completed `success` (Build/Unit/Lint/APK and API-35 emulator). Local FULL on the
  same head with `PULSELIBRE_VERSION_CODE=1989` also passed.
- Release APK: versionCode 1989, signing certificate SHA-256
  `36e7f3b664fedb04f175a97a45079a845d781a1769db1a4b02b1c8bf51439177`, matching
  the certificate reported for the build the GUI agent installed on S10.
- Device run: pending S10 lease handoff from GUI/#529. No preemption, no second
  dependency on that agent's test result.

## What the acceptance run must show

1. Before state: private product backup exported via the app, pulled only into a
   0700 host directory. No root, `run-as` or ADB access to app-private files is
   possible on this production build (`allowBackup=false`, not debuggable).
2. Exactly one historical download after the fix (automatic product entry or a
   single confirmed manual tap, never both). Terminal UI status is observed.
3. After state: second private backup export. Backup format 9 contains the new
   `downloaded_file_archive.jsonl` with complete original bytes, exact remote
   item references, whole-file SHA-256 checks and chunking below CursorWindow.
4. Offline comparison reports only aggregate counts. Recent-interval coverage is
   checked with the private FIT decoder on originals extracted from that backup.
5. No raw time series, identifiers, personal original bytes or screenshots enter
   Git. No reset, SetFlags, unpairing, second retry or new BLE commands.

## Honesty boundary

A successful bounded transfer is not automatically complete device-history
coverage. If the recent interval remains absent after the fix, the remaining
cause is the still-unverified transaction/filter context documented in
`CIRQA_RECENT_HISTORY_FRESHNESS_AUDIT_2026-11-23.md`, not a proven new protocol
selection. Any such result must be reported as unresolved, never as success.
