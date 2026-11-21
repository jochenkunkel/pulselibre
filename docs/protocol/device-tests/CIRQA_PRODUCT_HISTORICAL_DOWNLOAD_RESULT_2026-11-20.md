# CIRQA product historical download result (2026-11-20)

## Scope

- One user-confirmed run used the existing **Historie herunterladen** product
  action on the attached S25, after signed main build `2.0.0_build1552` was
  installed in place. The main CLOUD/FULL CI run was
  [44813726759](https://github.com/jochenkunkel/pulselibre/actions/runs/44813726759)
  for merge commit `0a5fdf6576a8cce6c07bdbebeb49593523a70fae`.
- No instrumentation test was run on the S25. No gate was reset and no second
  download attempt was made.
- The result below was read from the app's on-device final status. No raw files,
  payloads, identifiers, hashes, filenames, or private database contents are
  included here.

## Redacted result

- The app reported a drained run with no incomplete reason. The selected
  25-file queue completed successfully: **25 transferred, 25 verified, and 25
  privately stored** (488,067 bytes over 3,355 MLR chunks).
- **24 files were decoded and committed through the SQLite stage; 17 contained
  importable data, and 7 had no importable data.** No unsupported or failed
  files were reported.
- The app reported `validate=complete` for this run and
  `coverage=device-history-unverified`. These describe different things:
  transfer/file validation completed, but the device's full historical
  coverage was not established. The listing metadata did not establish a
  terminal history boundary (`session=false`, `nextTransaction=true`).
- At the time of the user's first check, the old **Verlauf** chart read recent
  BLE-received snapshots from `events` (service 6/19, `received_at_utc`), while
  FIT import wrote historical observations to the separate canonical
  `observations` table. This explained why those FIT rows were not visible in
  that chart; it was not evidence of a failed transfer or SQLite commit.
- After this report, GUI PRs #335 and #336 were merged into `main` (merge
  commits `7abb5cb` and `05078d9`). They add a separate FIT heart-rate history
  card to **Verlauf** and a separate FIT band-history line in the overview,
  both using measurement time and leaving the BLE-reception series separate.
  Signed-APK/S25 visual verification of those changes is separate and must not
  be inferred from the merge alone.
- The original coordination note records the issue as observed before that UI
  work: `docs/collaboration/agent-sync/2026-11-20_2208_opencode-an-claude_fit-observations-history-ui.md`.

## Limits and side effects

- This confirms that the existing product download path completed one bounded
  file queue and imported its importable FIT data. It does **not** prove that all
  historical files on the CIRQA were listed or downloaded.
- As disclosed in the product confirmation dialog, the accepted sync request
  may cause the band to mark buffered data as transferred. No delete, archive,
  `SetFlags`, reset, unpair, `CLOSE_ALL`, or additional device command was sent.
- Original files and device data remain local and private; none were committed
  or shared.
