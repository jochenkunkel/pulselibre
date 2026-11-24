# CI incident #48 – CSV export boundary assertion

Date: 2026-11-15. Failed workflow: https://github.com/jochenkunkel/pulselibre/actions/runs/43698764809

## Evidence
GitHub Actions build job failed at `:app:testDebugUnitTest`: 32 JVM tests executed, 1 failed (`RawDataExportTest.windowIncludesStartAndExcludesEnd`, line 14). Emulator job was skipped because build job failed. This is **not** evidence of an Android emulator crash.

## Root cause
The test asserted that the string `1970-01-01T00:00:03Z` must not occur anywhere in the exported CSV. However, the fixture for a reading measured at 2 seconds sets `receivedAtEpochMillis = time + 1000`, correctly producing 3 seconds in the `received_at_utc` column. The end-exclusive filter excludes readings *measured* at 3 seconds; it must not remove an earlier measurement merely because it was received at 3 seconds.

## Fix
Assert the excluded timestamp does not occur in the **measurement timestamp column** (the quoted value immediately followed by a comma), while explicitly asserting the receive timestamp remains present. Commit `e40931b`.

## Similar failures and controls
- **Cross-column false positives:** test structured CSV fields, not substring presence; this fix distinguishes the two timestamp positions.
- **Boundary errors:** tests cover inclusive start and exclusive end; additional timezone/UI conversion tests remain required.
- **Duplicate imports:** history merge unit tests cover idempotent replay; persistence transaction tests still required.
- **Malformed CSV / formula injection:** dedicated tests cover quotes, commas and spreadsheet formulas; comprehensive CSV round-trip tests remain to be added.
- **Invalid numeric values:** tests cover NaN exclusion; infinity and extreme timestamp tests are useful additions.
- **CI gating:** a failing unit test prevents emulator execution, correctly avoiding a misleading green build; no emulator result can be claimed for run #48.

## Verification status
The corrected test has been committed. A new GitHub Actions run must complete successfully before closing this incident. No local device installation is required.
