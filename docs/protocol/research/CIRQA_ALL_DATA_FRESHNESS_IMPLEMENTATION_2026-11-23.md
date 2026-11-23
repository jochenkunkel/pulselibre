# All-data freshness correction and acceptance plan

## Changes

- Always pull listed references within existing transfer/page/time budgets. Local
  hash integrity or repeated UID/type/size/flags never grants a remote skip.
  Repeated UIDs within one run are not silently discarded either.
- Preserve ItemReference transaction/modified metadata and exact encoded
  reference locally, including fields not yet understood.
- Canonical SQLite schema 10 stores complete received original bytes in
  `downloaded_files`, content-addressed by SHA-256, with exact remote reference
  versions in `downloaded_file_references`. No measurement interpretation is
  necessary for preservation. Existing valid local hash-named originals are
  archived by the existing offline background backfill.
- Unknown formats, status codes, unknown FIT fields and unsupported semantic
  records remain recoverable byte-for-byte in SQLite, separately from trusted
  physiological observations. Different contents of equal size remain distinct.
- Backup format 9 includes originals and references, chunked below CursorWindow
  and JSON-line limits. Restore checks whole-file hashes before replacement and
  restores the archive inside the existing atomic attached-DB transaction.
  Formats 1–8 remain readable. UI warns that originals may contain identifiers.

## Step 2: protocol comparison, no guessed active change

The prior root-cause audit compared ChatGPT issue #506 and the SHA-verified
Connect capture. Connect requests preserve agent-specific exclude_all_flags,
get_flags and included_data_types, unlike the product's token-only continuation.
Field presence alone does not identify the correct agent context for “all data”.
Copying an exclusion flag could reduce the returned set. No guessed UUIDs,
SetFlags, changed transport or extra bootstrap commands are enabled here.
The unfiltered first listing remains; bounded token follow-ups and honest
`device-history-unverified` reporting remain unchanged.

This separates the confirmed unsafe selection defect from the still-open
filter/session hypothesis. Successful subset transfer is not full-history proof.

## Acceptance

1. JVM regression: repeated metadata plus successful local import still pulls;
   changed transaction/modified-time and unknown reference fields are retained.
2. Real emulator SQLite: two same-size unknown payload versions survive,
   originals above 2 MiB export in bounded slices, backup/restore is byte-exact,
   truncated/corrupt original archives are rejected, schema 9→10 preserves events
   and existing opaque local originals are archived idempotently.
3. Full Android CI including emulator, R8 release, lint and coverage gates.
4. Check update signature/version continuity, then install without clearing data.
5. Under S10 lease, one controlled product sync and private before/after SQLite
   coverage comparison for the recent activity interval; no repeat download.

Only generated test data and aggregate acceptance results belong in Git.
Personal original bytes, exact health time series and remote references stay
local. Full device-history coverage must not be inferred from the new archive.
