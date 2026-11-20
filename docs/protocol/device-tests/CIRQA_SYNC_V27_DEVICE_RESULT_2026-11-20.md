# CIRQA V27 bounded device result (2026-11-20)

## Scope and release gate

- One V27 run was performed under the existing explicit authorization, after
  full Main CI run
  [44694993958](https://github.com/jochenkunkel/pulselibre/actions/runs/44694993958)
  passed for `f7a526fe62dc5e925f8053611873681a90df4c43`, including API-35
  instrumentation and coverage.
- The signed `de.pulselibre` build 1399 was installed in place over build 1382.
  The package name, higher version code, and signing certificate continuity
  were verified before installation. The app was not uninstalled and its data
  was not cleared.
- The pre-run local app backup was retained privately and ZIP-integrity checked.
  No backup, raw file, measurement, device identifier, hash, or detailed
  timestamp is included in this report.
- The continuous recorder was stopped through its app control before V27 and
  started again after the sync path terminated. Its running state was confirmed
  afterward.

## Redacted result

- Exactly one V27 one-shot attempt was used; the V25 gate was not changed.
- The device returned one complete listing page with 25 entries: 24 FIT files
  and one non-FIT file. All 25 selected transfers completed; failed, queued, and
  skipped counts were zero. Total selected transfer size was 488,067 bytes,
  below the 64 MiB bound; the 256-file bound was also respected.
- The sync report ended with the file queue fully processed, not with the
  120-second timeout. No retry was made. The final validation state was
  complete.
- The post-run app backup passed ZIP, manifest-size, record-count, and
  SHA-256-entry checks. Compared with the pre-run export, canonical observation
  rows increased by 27,442 and canonical FIT provenance rows increased by the
  same amount. Other database-table changes occurred in the same interval and
  are not attributed solely to V27.
- The app's read-only SQLite `quick_check` returned `ok`. An offline check of
  the post-run backup found no dangling canonical FIT-source, observation,
  provenance, metadata, or unmapped-field references. A direct
  `PRAGMA foreign_key_check` was not available through the production,
  non-debuggable package; no `run-as` or root access was used.
- The subsequent read-only in-app FIT inventory verified all 45/45 local FIT
  files. All 27,736 importable candidate keys were present in SQLite; zero
  were missing. This confirms local decoder-to-database coverage for the
  currently available verified files, not complete device-history coverage.
- Across those files the inventory counted 17,131 record messages and 29,365
  supported observations; 1,622 lacked timestamps and seven were outside the
  display range. These are aggregate decoder diagnostics, not a claim that all
  supported observations are importable or semantically complete.

## Limits and side effects

- A successful bounded page is not proof of complete device-history coverage.
  Issues #149, #150, and #144 remain open pending their other acceptance
  criteria.
- As disclosed before the run, the accepted sync request may cause the band to
  mark buffered data as transferred. No archive, file-flag, delete, reset,
  unpair, `CLOSE_ALL`, or other unrecognized device command was used.
- Original FIT/raw files and the unencrypted app backups remain local and
  private; none were committed or shared.
