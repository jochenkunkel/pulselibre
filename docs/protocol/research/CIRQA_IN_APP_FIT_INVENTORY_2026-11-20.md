# In-app FIT inventory and local reconciliation

Implemented on 2026-11-20 for #150, building on the read-only decoder and
redacted inventory introduced earlier.

## Behavior

The existing Testlabor → Synchronisation panel now inventories the app-private
`files/verified_fit` folder when the diagnostics snapshot is collected. It
validates regular FIT files, content-addressed filenames, FIT envelope/CRCs,
and supported observations using the existing offline decoder. It compares
only deterministic importable sample keys and source hashes against the
canonical `observations` and `fit_sources` tables. The UI and explicit
diagnostic export receive aggregate counts only.

No BLE operation, file write, FIT import, or upload is performed. The canonical
database file is opened with SQLite's explicit read-only flag (not through the
app's upgrade-capable `SQLiteOpenHelper`); only SELECTs and SQLite integrity
checks are issued. The user still explicitly initiates sharing after previewing the
diagnostic package. Raw sample values, timestamps, filenames, hashes, and
sample IDs are never placed in the report. If the private directory or
database cannot be read, the panel reports an unavailable status without
including exception details or paths.

The report is a local reconciliation of currently app-private files only. It
does not claim that all device history was downloaded. Direct `adb run-as`
access remains unavailable because the production package is not debuggable;
the in-app path was verified on the connected S25 instead.

## Live S25 verification

On 2026-11-20 the connected S25 had `2.0.0_build1380` installed. The existing
Testlabor was opened with its hidden long-press gesture and panel D was expanded.
The displayed aggregate-only result was:

- 40 FIT files seen, all 40 verified, 0 rejected.
- 294 importable deterministic sample keys; all 294 were present in the
  canonical `observations` table, with 0 missing keys.
- All 40 verified FIT sources were present in `fit_sources`.
- The database also reported 294 provenance rows and 110 FIT device-metadata
  rows. This inventory reports metadata-row counts, not the underlying values.

No diagnostic package was shared, no raw values/IDs/hashes were copied, and no
sync, BLE command, file write, or database write was performed. Screenshots
were inspected locally only and were not added to the repository. A prior
`run-as` denial is not evidence that the in-app path failed: the application
can read its own private files in its normal context.

## Issue acceptance review

| Issue | Evidence from this work | Remaining acceptance gaps | Status |
| --- | --- | --- | --- |
| #150 | Live S25 inventory through the safe in-app path; 40/40 FIT envelopes verified; 294/294 importable keys matched; 40/40 sources matched. | No per-file coverage/error matrix in the redacted UI; actual metadata field values and evidence-source matrix not audited; the stored set is not proof of full device-history coverage. | Keep open |
| #149 | Live match confirms no currently importable decoder candidate key is missing from the canonical observation store. | No production backfill/re-import was run; no full per-file semantic reconciliation, pipeline call-graph audit, or additional decoder-field validation in this work unit. | Keep open |
| #144 | The live diagnostic reads `observations`, `fit_sources`, and provenance counts without mutation. | No complete IST/SOLL ERD, storage decision, lossless migration plan, or architecture acceptance work was performed. | Keep open |

## Other verification

- Regression tests cover matching/missing database keys and sources and assert
  that hashes, sample IDs, and filenames do not appear in output.
- `:app:testDebugUnitTest`, `:app:lintDebug`, and
  `:app:assembleDebugAndroidTest` passed locally.
- Full PR and subsequent main CI, including API-35 emulator instrumentation,
  passed for PRs #270 and #273.
- Android instrumentation was not run on the S25; the live verification used
  only the in-app read-only diagnostics.
