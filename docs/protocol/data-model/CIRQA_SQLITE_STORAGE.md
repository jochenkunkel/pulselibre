# CIRQA local SQLite storage (current schema and target)

Stand: 2026-11-18. `CirqaDatabase` uses Android `SQLiteOpenHelper`; no cloud or
`INTERNET` permission is involved.

## Current persisted stores

There are currently two physical databases; the target architecture is not yet
migrated:

- `cirqa_measurements.db`, schema version 2:
  `events(id, line_sha256 UNIQUE, received_at_utc, dashboard_time, kind,
  service, payload_json)`. Indexes cover service/time, kind/time, and a partial
  dashboard-time index. Raw observed event details remain JSON by design.
- `pulselibre_readings.db`, schema version 2:
  `readings(device_id, metric, measured_at_ms, value, unit, sample_id,
  received_at_ms)` with composite primary key
  `(device_id, metric, measured_at_ms, sample_id)`, plus
  `sync_cursors(device_id, last_committed_ms)`, `fit_sources`, and
  `fit_observation_provenance`. The provenance table has foreign keys to the
  source hash and exact readings composite key. The v1→v2 upgrade only creates
  tables/indexes; existing v1 readings and cursors remain unchanged.

The stores, observed source-file relationship, proposed normalized schema, and
additive no-delete migration stages are described in the
[storage architecture ADR](CIRQA_STORAGE_ARCHITECTURE_ADR_2026-11-18.md).

## Writers, readers and retained legacy data

`CirqaLocalLog.append()/reading()` write realtime events to `events`. Dashboard,
statistics and JSONL/CSV exports read the canonical event database. CSV export
also imports rows from `pulselibre_readings.db` as `legacy_record`; the legacy
database is retained. FIT history currently writes validated observations to
`pulselibre_readings.db` through `LocalReadingStore.commitFitFile`. The reusable
offline `CirqaFitBackfill` uses the same validated/idempotent transaction,
recording source SHA-256, file size/path, decoder/profile version, and FIT
record/message/field coordinates for importable observations.

The former private `cirqa_overnight.jsonl` is removed on database open according
to the earlier explicit user decision. This is separate from the health-data
SQLite databases and user-triggered JSONL exports.

Verified FIT/raw files remain separate app-private originals, content-addressed
by SHA-256 and published atomically after integrity/size checks. The DB stores
FIT-source metadata keyed by hash, but original FIT/raw bytes are not included
in database backup archives.

## Backup, tests and limitations

Database backup format v2 serializes events, readings, cursors, FIT-source
metadata and field-level provenance into checksummed JSONL entries. Restore also
accepts version-1 archives; restoring one replaces current rows and supplies no
FIT provenance absent from that older archive. It is not encrypted and does not
include FIT/raw original bytes. Restore validates the archive before changing
rows. Do not manually copy a live SQLite main file without its journal.

GitHub full CI run 44405895254 passed on commit `a9a979c` (prior to the
provenance changes). Current provenance changes pass local JVM tests, APK and
Android-test APK builds, lint, and 52/52 API-35 emulator tests. Targeted
provenance/backup tests additionally cover v1→v2 preservation, transactional
FIT replay, backup v2 round-trip, v1 restore compatibility, FK checks and
rejection of orphaned provenance before restore confirmation.

Open: physical-store consolidation, migration of existing rows into an eventual
canonical database, provenance recovery for older FIT readings, bounded/
paginated large-history queries, and source-file backup/retention policy. No
unknown FIT field or clinical interpretation is inferred.
