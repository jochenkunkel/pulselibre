# Issue #637 – ChatGPT technical handoff (2026-11-25)

## Ask

Please independently review the failure analysis and suggest the smallest safe
fix / test for CIRQA history download, specifically whether concurrent
`SQLiteOpenHelper` opens during per-file archive/import explain the repeated
`SQLITE_BUSY` failures. Also review whether any remaining evidence supports a
Garmin Connect versus PulsLibre difference for *normal daily history*. Do not
infer missing Garmin field semantics or protocol behavior.

## State and user goal

Owner priority is complete local, lossless capture of all technically
accessible, permitted band data, including unknown fields; semantic validation
must not block raw retention. History sync is manual-only. A controlled S10
download is explicitly authorized under the S10 lease. Existing 120-second,
per-file and per-run bounds remain unchanged. Do not use destructive GFDI
operations, and do not treat the S25 as a test device.

PR #670 fixed several concrete issues and was merged as `be536f74`; PR-head
QUICK and dependency checks passed. The post-merge QUICK build #45679360777
completed successfully on `main` `4f5566ad`; signed APK version 2250 was
verified and installed in-place on the S10 with signer continuity. The build
did not include later source changes now present on `origin/main`.

## Latest controlled S10 result

Run #3 was executed once, under one S10 lease, with a fresh pre-run backup,
one manual confirmation, a new-run-ID check (to reject a stale screen report),
a terminal report, and a fresh post-run backup. The 120-second application
budget was not changed. Lease ended `FREI`.

Redacted terminal evidence:

- 1 item-list response, 29 listed, 29 selected, 29 pull requests queued.
- `runDrained=false`, `incompleteReason=session-time-limit`,
  `coverage=device-history-unverified`.
- 28 transfers completed with exact-size/CRC verification and private archive;
  25 decoded; 24 SQLite-committed; 18 imported; 6 had no importable data.
- One FIT import/commit failure classified as
  `FIT_IMPORT_COMMIT/SQLiteDatabaseLockedException`.
- At timeout, an item with expected length 6,010 bytes had received 0 bytes.
- Thus 29/29 complete files and full device-history coverage remain unproven.

Backup v11 before/after:

| Counter | Before | After |
| --- | ---: | ---: |
| verified archived originals | 2,844 | 3,054 |
| original bytes | 984,816 | 1,009,688 |
| verified FIT originals | 37 | 37 |
| archive reference rows | 2,850 | 3,060 |
| canonical observations in the audited export | 0 | 0 |

Manifest and archive-group validation passed. Diff was +210 originals and
+210 references, but no additional FIT originals or canonical observations.
These aggregate counts are from private backup audits; no raw data, file IDs,
hashes, device identifiers, or health values are included here.

Previous run #2 on the same build also hit `session-time-limit`: 29 selected,
28 transferred, 25 verified/archived, 23 decoded/committed, 17 imported,
6 no-import-data, and 3 original-archive `SQLiteDatabaseLockedException`s.
The third run therefore moved the failure boundary: archive recovery/retries
retained bytes, but a later FIT import still failed once.

Private evidence (run reports, backups, HCI and bugreport captures) remains in
access-controlled local storage and is not included or linked here. Do not
attach it to a public issue or PR.

## Concrete SQLite evidence / likely defect

The latest UI terminal reporter classifies the failure at the outer FIT
import/commit boundary. That boundary currently includes all of:

1. constructing/opening a fresh `CirqaDatabase` helper;
2. `CirqaFitBackfill.importDecodedVerifiedFile(...)`;
3. FIT decoder/source checks and `FitObservationCommitter.commit(...)`;
4. closing the helper.

The catch in `CirqaMlHandshakeProbe.kt` around lines 3429–3465 maps any
exception in that whole block to `FIT_IMPORT_COMMIT`; it does not prove the
exception was thrown by `beginTransaction`, an INSERT, or COMMIT.

Private Logcat repeatedly records Android SQLite open failures for the app DB
at `PRAGMA journal_mode` (and sometimes `PRAGMA cache_size`), with stack frames
through `SQLiteConnection.setWalModeFromConfiguration`,
`SQLiteConnectionPool.openConnectionLocked`, and `SQLiteOpenHelper`. This is
an *open/configuration* failure, not an SQL statement inside
`FitObservationCommitter.commit`. Stack frames are R8-obfuscated in the release
APK; the mapping file was not found in the downloaded CI artifact. The exact
open failure could not yet be matched to a source-level frame for the one
reported FIT failure.

Relevant source facts at post-merge `origin/main` (`4f5566ad`):

- `CirqaDatabase.archiveDownloadedOriginal(...)` uses a static
  `downloadedFileArchiveLock`, a content-addressed fsync recovery journal, and
  `CirqaSqliteBusyRetry`.
- The static archive API holds that lock from `CirqaDatabase(context)` creation
  through helper close; the instance archive method also synchronizes its
  archive transaction. The lock serializes archive operations with one another,
  but does not include `CirqaFitBackfill` / `FitObservationCommitter` writes.
- `CirqaMlHandshakeProbe` performs archive, FIT verification/decode, then opens
  another `CirqaDatabase(context)` for the import, outside the archive lock.
  The BLE GATT callback is configured with the probe's serial `HandlerThread`,
  and completed-transfer processing is posted to that serial handler. The
  checked source therefore does **not** establish that successive file
  completions race each other on different callback threads.
- Each `CirqaDatabase(context)` is a new `SQLiteOpenHelper`. Android's helper
  synchronization is instance-local. Although the serial BLE pipeline itself
  narrows per-file overlap, the archive lock does not serialize helper opening
  with the import helper opening/transaction, nor does it cover other database
  users (for example backup/export or app workers).
- The app log specifically shows `SQLITE_BUSY` while opening/configuring WAL.
  SQLite docs distinguish this from corruption; busy means conflicting
  database activity. The hypothesis is therefore a process-local concurrent
  open/configuration race between archive/import (or another DB writer), not
  malformed FIT bytes or a CIRQA transfer CRC issue.

**Best current hypothesis (not yet proven):** the exact source initiating the
lock is unknown. The import helper opens the same DB outside the archive
serialization boundary, so an archive, backup/export, or another writer can
overlap that open/configuration step even though BLE completion processing is
serialized. This can produce `PRAGMA journal_mode` SQLITE_BUSY while another
helper is active. The exception is then misleadingly labeled
`FIT_IMPORT_COMMIT`. A per-file callback-thread race is not established by
the inspected source.

Alternative contributors to rule out: the backup/export UI or another app
worker opening/writing the same DB during the sync; a stale/open transaction;
and whether the failed source was retained in the private pending-original
journal. The post-run backup inventory is aggregate-only and does not identify
the particular source. Do not infer an item identity from private hashes.

### Additional private-log correlation (2026-11-25, read-only)

The protected run-3 Logcat was re-correlated against file modification times
and the terminal report, without copying raw lines or identifiers here:

- The repeated `PRAGMA journal_mode` / `PRAGMA cache_size` failures are logged
  under the app's process ID, on four distinct app threads; the final open-path
  exception occurs at device-local 20:22:08 (19:22:08 UTC), shortly before the
  terminal UI snapshot at device-local 20:22:12. The captured failures are
  logged in the app process; they do not demonstrate a separate app process,
  but also do not identify which operation (or connection) held the conflicting
  lock.
- Earlier retry clusters occur at approximately device-local 20:20:26–20:20:37
  and 20:21:58–20:22:08 (UTC 19:20–19:22). Repeated open/configuration failures
  on multiple threads are consistent with concurrent helper/database activity,
  but thread IDs and timestamps alone do not prove the specific
  archive/import/backup operation or the owner of the SQLite lock.
- The captured release stack reaches Android's SQLite open/configuration path;
  app frames are R8-obfuscated and the matching mapping file is unavailable.
  No source-level caller or per-file identity can therefore be assigned to
  the terminal failure. The UI's broad `FIT_IMPORT_COMMIT` catch still spans
  helper open, verified import/commit, and helper close.

This narrows—but does not establish—the process-local contention hypothesis.
It does **not** justify a product synchronization change yet. A next offline
test should deterministically exercise Android `SQLiteOpenHelper` opens while
a separately held connection/transaction is active, record thread-safe stage
boundaries (open, source verification, transaction, close), and assert that
retry is bounded and does not duplicate FIT writes. First verify whether the
observed Android open/configuration failure is reproducible on the emulator;
do not assume the Python SQLite experiment models Android's WAL configuration.

### Follow-up: API-35 emulator reproduction and bounded recovery

On 2026-11-25, a focused instrumentation probe on the API-35 emulator held a
write transaction on one `CirqaDatabase` connection while opening fresh
`CirqaDatabase` helpers against the same synthetic database. Fresh helper opens
reproduced `SQLiteDatabaseLockedException`; aggregate emulator Logcat diagnostics
included `SQLITE_BUSY` at `PRAGMA journal_mode`. This demonstrates that the
reported Android open/configuration failure is reproducible under controlled
same-process SQLite contention. It does not identify the lock holder or prove
that this exact interleaving occurred during S10 run #3.

The probe then applied the existing bounded SQLite busy retry to the open/read
operation. Once the test released the synthetic writer, the retry succeeded;
the focused API-35 instrumentation test passed. No band, S10, or BLE access was
involved.

A minimal defensive change now wraps the FIT helper-open + verified import
boundary in one `runOnce` retry (250 ms delay, retrying only
`SQLiteDatabaseLockedException`). Retrying that whole boundary is safe because
each FIT source import is transactional and idempotent; unrelated exceptions
are not retried. The helper is explicitly opened before the import call so
reporting distinguishes `SQLITE_OPEN` from `FIT_IMPORT_COMMIT`. Persistent
contention still fails visibly after the single retry; existing sync/run
budgets are unchanged. This is a bounded mitigation, not proof of the exact
run-3 source or lock owner.

Focused validation on API 35 passed:

- `CirqaSqliteBusyRetryTest` and `CirqaSyncFileCompletionTest`.
- `CirqaSqliteOpenContentionTest` (synthetic helper-open contention/recovery).
- `CirqaFitBackfillInstrumentedTest` (2/2 existing transactional/idempotence
  cases).

## Existing network / reverse-engineering evidence

- Local comparison report:
  `docs/protocol/research/CIRQA_CONNECT_NORMAL_HISTORY_COMPARISON_2026-11-25.md`.
- Private c16 HCI replay: initial ItemList was fully reconstructed and
  contained 38 entries; three full transaction-follow-up responses each
  contained zero entries and repeated the same next-transaction token.
  This cannot distinguish “no more entries” from filter/transaction semantics.
- Private c17 Connect capture: at least one complete CRC-valid FIT content was
  reconstructed from MLR; a complete FileAccess/request-to-item correlation
  is absent. Existing FIT evidence includes Daily-History-like Global Message
  398 and opaque Global 397, but no proof of full normal-history sync.
- Garmin FIT SDK Tools profile 21.222.0: Global 398 is
  `skin_temp_overnight`; Global 397 is not defined. Current app correctly
  retains unknown bytes without mapping them to unverified health metrics.
- Public Gadgetbridge Garmin protocol documentation is a useful *limited
  comparator*: its described capture is Garmin Connect Android 4.66 with a
  Forerunner 245 and legacy ML/GFDI; it explicitly says the newer handle-based
  protocol is unsupported. It is not proof of CIRQA MLR/FileSync semantics.
  https://gadgetbridge.org/internals/specifics/garmin-protocol/
- Android SQLiteOpenHelper AOSP source:
  https://android.googlesource.com/platform/frameworks/base/+/refs/heads/main/core/java/android/database/sqlite/SQLiteOpenHelper.java
- SQLite result-code reference (SQLITE_BUSY):
  https://sqlite.org/rescode.html#busy

Local search found JADX tooling but no Garmin Connect APK/decompiled private
source tree in the checked home paths. No proprietary app was modified,
redistributed, or represented as analyzed. Public reverse-engineered
Gadgetbridge documentation and the project’s private HCI captures were used as
the available comparison materials.

### Controlled follow-up after PR #678 (2026-11-25, one S10 run)

One manual-only S10 run was performed under a single lease after validating
the signed main APK (version 2271, matching installed signer) and creating a
fresh, locally verified pre-run backup. No retry or second BLE run occurred;
the lease was released (`FREI`).

The terminal report lists 32 selected files and 32 pull requests, then ends
with `runDrained=false`, `session-time-limit`, and
`coverage=device-history-unverified`. Stage counters: 31 transfer-complete,
30 integrity-verified, 31 privately stored, 26 decoded/SQLite-committed,
19 imported, 7 with no importable data, and one
`ORIGINAL_ARCHIVE/SQLiteDatabaseLockedException`. There were no transaction
follow-ups. The previous fix retries a bounded SQLite lock at the FIT
helper-open/import boundary; it does not make archive-stage contention
impossible. The lock holder and exact cause of this archive failure are not
established. The complete original exception text and identifiers remain
private.

Backup v11 before/after:

| Counter | Before | After | Delta |
| --- | ---: | ---: | ---: |
| Verified archived originals | 3,054 | 3,242 | +188 |
| Original bytes | 1,009,688 | 1,065,612 | +55,924 |
| Verified FIT originals | 37 | 39 | +2 |
| Archive reference rows | 3,060 | 3,248 | +188 |
| `canonical_observations` table rows | 29,123 | 29,720 | +597 |
| FIT generation observations | 57,687 | 58,284 | +597 |
| FIT observation provenance rows | 29,123 | 29,720 | +597 |
| `downloaded_file_archive` export rows | 6,115 | 6,491 | +376 |
| Import error rows | 25 | 30 | +5 |

Both backups passed manifest and archive-group validation (zero failures).
The SQLite difference confirms a persistence increase, not full history
coverage or independent semantic validation. The audit script's legacy summary
counter `canonical_observation_rows` remained 0 because it only recognizes
older table names; the version-11 table inventory above is authoritative for
these deltas. The diff is between snapshots, not a per-run attribution:
app-start archive recovery/backfill can also change inventory, so the added
rows cannot all be assigned to the 32 selected files. The error count is
aggregate and does not assign the added rows to an individual file. Run-level
counts and the per-file report are preserved in protected local storage only.

The required QUICK and dependency gates for #678 passed before the run.
Main FULL/API-35 run `45719176447` on `0d894299` was still in progress when
this handoff was drafted; FULL is not a merge gate under the current owner
policy. No Garmin Connect comparison or complete historical coverage is
established by this follow-up.

## Questions for ChatGPT review

1. Is a narrowly scoped process-wide lock around each completed file's
   archive→FIT-import DB writes the right correction, or should all
   `CirqaDatabase` helper opens share a singleton/ref-counted helper?
2. Should `CirqaSqliteBusyRetry` wrap the complete import open+transaction
   boundary as defense-in-depth? What retry envelope can fit the existing
   120-second cap without hiding persistent failure?
3. How can the test deterministically reproduce Android's
   `SQLiteOpenHelper`/WAL `PRAGMA journal_mode` race, rather than merely
   injecting an exception into a helper?
4. Should failure reporting distinguish “DB helper open”, “source read”,
   “FIT decode”, “transaction begin/write/commit”, and “helper close”, so the
   next device report identifies the actual fault boundary?
5. Once the database path is corrected, what *offline-first* evidence is
   sufficient before considering another controlled S10 run? No BLE test or
   limit increase is requested by this handoff.
6. Separately, what minimum request/response evidence is needed to compare
   Garmin Connect normal daily history with PulsLibre, without equating a
   complete FIT transfer or CSV delta with full history?

## Next technical steps proposed

1. Preserve the new log correlation as evidence, while keeping the lock owner
   and failing FIT item explicitly unknown.
2. Add an Android instrumentation/emulator reproduction for concurrent helper
   open/configuration and an explicit failure-stage classification test; a
   JVM-injected exception alone is insufficient.
3. Only if reproduced, implement the smallest serialization/retry correction
   without changing BLE commands, timeout, download budgets, FIT mapping, or
   backup format.
4. Run focused tests and required QUICK/dependency gates. No FULL-CI or device
   test is a merge gate under the current owner policy.
5. Only after offline evidence and a suitable green build, decide whether a
   further controlled S10 test is useful; do not start one automatically.

## Independent error audit (2026-11-26, offline)

- **No full-history result was misstated in the terminal summary or this
  handoff:** run 4 is explicitly incomplete (`runDrained=false`,
  `device-history-unverified`). The 31 private-storage successes include the
  original whose SQLite archive failed: the exact bytes were retained in the
  private `downloaded_originals_pending` journal. This is recoverable local
  storage, but not evidence that the required SQLite archive/inventory write
  succeeded.
- **Retry scope is narrower than the broad “SQLite busy” name suggests.**
  `CirqaSqliteBusyRetry.run` surrounds the original-archive call, and
  `archiveDownloadedFile` has its own process-wide monitor and pending journal.
  The bounded retry was therefore attempted for the run-4 archive failure;
  it was exhausted. `runOnce` surrounds FIT helper-open/import. Neither retry
  proves the lock holder or fixes persistent contention. The default predicate
  currently recognizes `SQLiteDatabaseLockedException` only, not the distinct
  Android `SQLiteBusyException`; regression tests inject a custom predicate and
  do not test either real exception against the defaults. Thus the implementation
  does not yet cover both named SQLite BUSY/LOCKED exception classes.
- **Potential contention remains, not a confirmed root cause:** the archive
  monitor serializes archive callers, but FIT imports and other database
  writers do not take that monitor. Their overlap is a plausible source of
  contention; available evidence does not establish that it caused this
  particular exception. The evidence PR #669 separately reports three archive
  failures on its earlier run; do not combine those counts with run 4.
- **Audit-script summary defect:** its legacy `canonical_observation_rows`
  and `fit_source_rows` counters return zero for v11 because they recognize old
  export names. The per-table row counts and the explicit v11 table-name
  deltas in the table above are correct; these zero summary values must not be
  used as evidence of empty canonical tables. This naming defect was already
  disclosed in the preceding handoff text.
- The +188 archive count, +2 FIT originals, +597 generation-observation rows,
  and +597 provenance rows are snapshot differences, not run-attributed
  results. The archive validation covers SQLite archive chunks, not a complete
  inventory of every separate private file store. No evidence here proves
  Garmin Connect parity or device-history completeness.

**Audit conclusion:** the remaining actionable code gap is exception
classification coverage (and a regression test using actual Android exception
types); the observed run-4 archive failure itself is already bounded, visible,
and journaled. Do not claim its cause fixed or use these findings to justify
another device run. Address the classification gap in a focused code change
before relying on retry for Android `SQLITE_BUSY` cases.
