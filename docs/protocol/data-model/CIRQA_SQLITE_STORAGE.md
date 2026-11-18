# CIRQA local SQLite storage (current schema and target)

Stand: 2026-11-18. `CirqaDatabase` uses Android `SQLiteOpenHelper`; no cloud or
`INTERNET` permission is involved.

## Current persisted stores

There are currently two physical databases; the target architecture is not yet
migrated:

- `cirqa_measurements.db`, schema version 3:
  `events(id, line_sha256 UNIQUE, received_at_utc, dashboard_time, kind,
  service, payload_json)` plus normalized `observations`, `sync_cursors`,
  `fit_sources`, and `fit_observation_provenance`. Indexes cover event
  service/time, kind/time, dashboard-time, observation time and FIT provenance.
  Raw observed event details remain JSON by design.
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

Database backup format v3 serializes events, both historical and canonical
observations/cursors, FIT-source metadata and field-level provenance into
checksummed JSONL entries. Restore accepts v1 and v2 archives as well; those
versions have no canonical normalized snapshot, so restore rebuilds the
copy-stage marker only for v3. The archive is not encrypted and excludes FIT/raw
original bytes. Restore validates checksums, record schemas and FIT foreign-key
relationships before confirmation. Do not manually copy a live SQLite main
file without its journal.

GitHub full CI run 44414867187 is for schema-v3 copy work before canonical
write/backup v3 changes. Current canonical write/backup changes pass JVM tests,
debug APK, lint, Android-test APK, 59/59 API-35 emulator tests and 19/19 Python
tests. GitHub CI for these latest changes is still pending.

Schema-v3 `ensureHistoricalStoreCopied()` copies and verifies the old history
without deleting it. New FIT backfill writes target the canonical DB after this
copy; backup v3 includes both stores and can restore older v1/v2 archives.
Remaining: physical retirement of the old DB after a verified transition,
dashboard readers/repository integration, broader migration/restore fixtures,
provenance recovery for old FIT rows, bounded/paginated history queries, and
original-file backup/retention policy.
