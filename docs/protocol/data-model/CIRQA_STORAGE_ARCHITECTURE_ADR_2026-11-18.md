# ADR: CIRQA local storage and additive consolidation

- Date: 2026-11-18
- Status: Proposed target; current schema and migration status are stated separately.
- Scope: SQLite event/history stores and immutable private CIRQA file originals.

## Decision

Keep one app-private database file as the long-term target: `cirqa_measurements.db`.
It will contain both protocol/event evidence and normalized measurements. Retain
FIT and non-FIT originals as separate immutable, content-addressed files; SQLite
stores their hashes, format, size, validation/import status, and relative path.
This avoids copying binary payloads into SQLite while allowing measurements to
reference their exact source.

Do not convert all protocol evidence into relational columns: `events.payload_json`
is a deliberate flexible envelope for versioned, observed protocol messages.
Measurements used for queries, dashboards, exports, and scientific work belong
in typed relational tables. Future aggregate/rollup data for #140 belongs in the
same database, keyed by metric, time bucket, aggregation method/version, and
source range; no separate aggregate database is warranted at current scale.

This decision is **not yet a completed migration**. The app currently has two
SQLite files. The legacy file must not be deleted or rewritten automatically.

## Verified current state

```mermaid
erDiagram
    CIRQA_DB ||--o{ EVENTS : stores
    LEGACY_DB ||--o{ READINGS : stores
    LEGACY_DB ||--o{ SYNC_CURSORS : stores
    APP_FILES ||--o{ VERIFIED_FIT : contains
    APP_FILES ||--o{ VERIFIED_RAW : contains

    CIRQA_DB {
      integer schema_version "2"
    }
    EVENTS {
      integer id PK
      text line_sha256 UK
      text received_at_utc
      text dashboard_time
      text kind
      integer service
      text payload_json
    }
    LEGACY_DB {
      integer schema_version "1"
    }
    READINGS {
      text device_id PK
      text metric PK
      integer measured_at_ms PK
      real value
      text unit
      text sample_id PK
      integer received_at_ms "nullable"
    }
    SYNC_CURSORS {
      text device_id PK
      integer last_committed_ms
    }
    VERIFIED_FIT {
      text sha256 PK
      blob bytes "private file, not DB"
    }
    VERIFIED_RAW {
      text sha256 PK
      blob bytes "private file, not DB"
    }
```

Observed constraints/indexes: `events.line_sha256` is unique;
`events_service_time(service, received_at_utc)`,
`events_kind_time(kind, received_at_utc)`, and partial
`events_dashboard_time(service, dashboard_time DESC, id ASC)` are indexed.
`readings` has composite primary key `(device_id, metric, measured_at_ms,
sample_id)` and `readings_by_time(measured_at_ms)`. `sync_cursors.device_id`
is the primary key. Current schemas do not declare relational FKs between
readings and source FIT files. FIT source hash and record/field coordinates
exist in the decoder result and are reduced to a stable `sample_id` in the
current SQLite import.

The 18 Nov 2026 S25 snapshots passed SQLite `integrity_check`. Observed counts
and privacy-limited reconciliation are in
`../research/CIRQA_S25_OFFLINE_RECONCILIATION_2026-11-18.md`.

## Target model (not current production DDL)

```mermaid
erDiagram
    DEVICES ||--o{ DEVICE_METADATA_OBSERVATIONS : observes
    DEVICES ||--o{ SYNC_SESSIONS : participates
    DEVICES ||--o{ DEVICE_FILE_REFS : lists
    SOURCE_FILES ||--o{ DEVICE_FILE_REFS : references
    SOURCE_FILES ||--o{ IMPORT_RUNS : processed_by
    IMPORT_RUNS ||--o{ IMPORT_ERRORS : records
    SOURCE_FILES ||--o{ OBSERVATIONS : supplies
    METRICS ||--o{ OBSERVATIONS : describes
    DEVICES ||--o{ OBSERVATIONS : measured_by
    DEVICES ||--o{ SYNC_CURSORS : checkpoints
    METRICS ||--o{ SYNC_CURSORS : checkpoints
    DEVICES ||--o{ EVENTS : emits
    METRICS ||--o{ AGGREGATES : summarizes

    DEVICES {
      text device_key PK "local pseudonymous key"
      text model "nullable, observed not inferred"
      text created_at_utc
    }
    DEVICE_METADATA_OBSERVATIONS {
      integer id PK
      text device_key FK
      text key
      text value "private; typed/value policy by key"
      text observed_at_utc
      text source
      text freshness
    }
    SOURCE_FILES {
      text sha256 PK
      text format
      integer byte_length
      integer header_crc_valid
      integer file_crc_valid
      text relative_path
      text validation_status
      text created_at_utc
    }
    DEVICE_FILE_REFS {
      text device_key PK,FK
      text source_sha256 PK,FK
      text remote_ref "nullable, sensitive, local only"
      text first_seen_at_utc
    }
    METRICS {
      text metric_key PK
      text canonical_unit
      text profile_reference
      integer profile_version
    }
    OBSERVATIONS {
      text idempotency_key PK
      text device_key FK
      text metric_key FK
      integer measured_at_ms
      real value
      text unit
      text quality
      text source_sha256 FK
      integer record_ordinal
      integer global_message_number
      integer field_number
      integer received_at_ms
      text decoder_version
    }
    SYNC_SESSIONS {
      text session_id PK
      text device_key FK
      text protocol
      text started_at_utc
      text finished_at_utc
      text status
    }
    SYNC_CURSORS {
      text device_key PK,FK
      text metric_key PK,FK
      text protocol PK
      integer last_committed_ms
    }
    EVENTS {
      integer id PK
      text device_key FK
      text line_sha256 UK
      text received_at_utc
      text kind
      integer service
      text payload_json
    }
    IMPORT_RUNS {
      text run_id PK
      text source_sha256 FK
      text decoder_version
      text started_at_utc
      text finished_at_utc
      text status
    }
    IMPORT_ERRORS {
      integer id PK
      text run_id FK
      text error_code
      integer record_ordinal
      integer field_number
    }
    AGGREGATES {
      text metric_key PK,FK
      integer bucket_start_ms PK
      text bucket_kind PK
      text method_version PK
      real value
      integer sample_count
    }
```

Implementation DDL must use `PRAGMA foreign_keys=ON`, `CHECK` constraints for
nonnegative timestamps and finite/valid values, explicit `NOT NULL`, and indexes
on `(device_key, metric_key, measured_at_ms)`, `(source_sha256, record_ordinal)`,
and session/status/time queries. A device and metric alone do not prove sample
identity. Content-file deduplication is by SHA-256; observation deduplication is
by decoder-derived stable idempotency key. Exact timestamp/value matches are
retained unless an explicit scientifically reviewed rule says otherwise.
Profile field definitions remain traceable to the pinned Garmin profile release;
the app does not need to copy the entire external profile into each database.

## Data lifecycle and clocks

1. V24/V25 experimental transfer receives complete bounded file bytes.
2. FIT files pass envelope/header/file CRC checks and are atomically published
   as `<sha256>.fit`; raw files pass their exact announced size and are published
   as `<sha256>.bin`. Incomplete files are not published.
3. Decoder emits only explicitly supported profile fields. Unknown fields remain
   skipped/recorded as inventory evidence, not guessed.
4. Validation filters missing timestamps and non-importable quality values.
5. A transaction inserts a file's observation batch and advances only its
   applicable cursor after successful commit. Replaying the file uses stable
   source/record/field keys and does not create duplicates.
6. Dashboard/statistics/export query normalized observations; raw protocol
   evidence remains separate. Backups serialize both observations and events
   and verify counts/checksums before restore confirmation.

`measured_at_ms` is the device/FIT measurement clock. `received_at_ms` is the
local transfer/import clock and must not replace a missing measurement time.
`observed_at_utc` is when metadata was actually seen. A file-list transaction
identifier is not a committed SQLite cursor.

## Migration and rollback plan

1. Create an app-private backup and record old schema versions, row counts,
   primary-key uniqueness, cursor values, and original file hash/size manifest.
   Do not reset, rename, or delete either database/file store.
2. Add target tables/indexes using a monotonic `CirqaDatabase` version upgrade.
   The upgrade is additive and runs in one transaction.
3. Copy legacy readings and cursors with exact values and stable keys. Use
   `INSERT OR IGNORE` only where the destination key exactly matches the source
   identity; verify source/destination count and key coverage before marking the
   migration complete. Keep the old database intact and read-only as rollback.
4. Make migration restartable: a marker is written only after successful
   validation. Interrupted transactions roll back; a subsequent open retries.
5. Route new FIT imports through the canonical repository only after migration
   has completed. Preserve version-1 backup restore compatibility and extend the
   backup manifest/version to include source files/observations before relying
   on the consolidated store.
6. Run backup→restore round trips against both pre-migration and migrated
   fixtures. Compare counts, stable keys, cursors, event hashes and file hashes.
   Retire neither legacy DB nor original files as part of this migration.

The old app version must fail safely when opening a newer DB rather than
downgrading or replacing it. Rollback is to restore a verified backup with a
compatible app version; copying a live SQLite main file without its journal is
not a valid backup procedure.

## Operational/privacy decisions

- Keep the database and original files app-private. No cloud, telemetry, or
  committed health data. Backup v1 is checksummed but **not encrypted**; do not
  claim encryption. Encryption-at-rest for user-selected backups requires a
  separate key-management decision.
- Keep content-addressed originals immutable. Quotas/orphan cleanup require
  explicit policy and must never delete user health data implicitly.
- Bound each file and batch, decode and commit incrementally, index time windows,
  and paginate large result sets. Avoid whole-history materialization in UI.
- Room is not selected solely for style: the existing SQLite/OpenHelper path is
  small and explicit. Reconsider if query/schema complexity justifies generated
  migrations and compile-time DAO checks.

## Remaining acceptance work

The diagrams and target/migration decision are architecture artifacts. They do
not themselves implement migration, provenance FKs, metadata observations, or
backup-format v2. Those remain required before closing issue #144.
