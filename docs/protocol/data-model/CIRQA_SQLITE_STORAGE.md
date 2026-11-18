# CIRQA local SQLite storage (current schema and target)

Stand: 2026-11-19. `CirqaDatabase` uses Android `SQLiteOpenHelper`; no cloud or
`INTERNET` permission is involved.

## Current persisted stores

There are currently two physical databases; the target architecture is not yet
migrated:

- `cirqa_measurements.db`, schema version 4:
  `events(id, line_sha256 UNIQUE, received_at_utc, dashboard_time, kind,
  service, payload_json)` plus normalized `observations`, `sync_cursors`,
  `fit_sources`, `fit_observation_provenance`, and `fit_device_metadata`.
  Indexes cover event service/time, kind/time, dashboard-time, observation time,
  FIT provenance, and metadata time. The metadata table contains only
  whitelisted FIT `device_info` fields; serial numbers and device indexes are
  excluded.
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
also includes canonical FIT history and imports rows from
`pulselibre_readings.db` as `legacy_record`; the legacy database is retained.
The reusable offline `CirqaFitBackfill` writes validated observations,
whitelisted FIT `device_info` fields, source SHA/file metadata, provenance, and
cursor atomically to the canonical database after the historical-copy check.
FIT codes are stored as codes, not asserted as a verified CIRQA identity or
current live state.

The former private `cirqa_overnight.jsonl` is removed on database open according
to the earlier explicit user decision. This is separate from the health-data
SQLite databases and user-triggered JSONL exports.

Verified FIT/raw files remain separate app-private originals, content-addressed
by SHA-256 and published atomically after integrity/size checks. The DB stores
FIT-source metadata keyed by hash, but original FIT/raw bytes are not included
in database backup archives.

## Backup, tests and limitations

Database backup format v4 serializes events, historical and canonical
observations/cursors, FIT-source metadata, field-level provenance, and canonical
FIT device metadata into checksummed JSONL entries. Restore accepts v1–v3
archives; older versions contain no device-metadata entry. The archive is not
encrypted and excludes FIT/raw original bytes. Restore validates checksums,
record schemas and FIT foreign-key relationships before confirmation. Do not
manually copy a live SQLite main file without its journal.

GitHub full CI run 44417266709 passed the canonical write/backup v3 change
before this schema-v4 metadata work. Current local verification passes 628/628
JVM tests, 63/63 API-35 emulator tests, 19/19 Python tests, debug APK, lint and
Android-test APK. GitHub CI was not started for schema v4 because the user has
temporarily paused the workflow.

Schema-v3 `ensureHistoricalStoreCopied()` copies and verifies the old history
without deleting it. New FIT backfill writes target the canonical DB after this
copy; schema-v4 metadata is additive. Backup v4 includes both stores and
restores older v1–v3 archives. Remaining: offline re-backfill of existing FIT
files for metadata recovery, physical retirement of the old DB after a verified
transition, dashboard readers/repository integration, broader migration/
restore fixtures, provenance recovery for old FIT rows, bounded/paginated
history queries, and original-file backup/retention policy.
