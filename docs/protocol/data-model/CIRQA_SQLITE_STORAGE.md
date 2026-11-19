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

`HistoricalObservationRepository` is the schema-independent read API for
normalized historical rows. It accepts a half-open time window, optional metric
and device filters, and a bounded keyset page; records may include nullable
FIT-field provenance (legacy rows have no provenance). It first runs/verifies
the idempotent historical copy and does not delete or mutate source rows.
Dashboard consumers have not yet been migrated from event-only reads to this
API, so the FIT history is available through the repository and export, not
claimed as visible in dashboard charts.

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

GitHub full CI run 44417266709 passed the canonical write/backup v3 change.
After schema-v4 metadata work and repository additions, full GitHub CI run
44473008816 passed for PR #170 (`f40b20d`), including build/JVM/Python/lint,
instrumentation APK, and API-35 emulator tests. Local verification also passed
629/629 JVM, 65/65 emulator and 19/19 Python tests after adding the paginated
repository contract and its migration/provenance coverage.

Schema-v3 `ensureHistoricalStoreCopied()` copies and verifies the old history
without deleting it. New FIT backfill writes target the canonical DB after this
copy; schema-v4 metadata is additive. Backup v4 includes both stores and
restores older v1–v3 archives. Remaining: offline re-backfill of existing FIT
files for metadata recovery, physical retirement of the old DB after a verified
transition, dashboard consumer integration with `HistoricalObservationRepository`,
broader migration/restore fixtures, provenance recovery for old FIT rows, and
original-file backup/retention policy.
