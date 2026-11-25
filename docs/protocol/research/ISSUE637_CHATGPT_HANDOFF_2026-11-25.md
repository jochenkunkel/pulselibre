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

1. Correlate the private log timestamps with archive/import/backup activity;
   inspect source-level call stacks on the exact `origin/main` revision. The
   BLE completion path is serial, but other DB users remain to be checked.
2. Add a deterministic concurrency regression for concurrent archive/import
   helper opens and a failure-stage classification test.
3. Implement the smallest serialization/retry correction without changing BLE
   commands, timeout, download budgets, FIT mapping, or backup format.
4. Run focused JVM/SQLite tests and required QUICK/dependency gates. No FULL-CI
   or device test is a merge gate under the current owner policy.
5. Only after offline evidence and a suitable green build, decide whether a
   further controlled S10 test is useful; do not start one automatically.
