# CIRQA local SQLite storage v1

Implemented in `CirqaDatabase.kt`. Android's built-in SQLiteOpenHelper, no cloud, no INTERNET permission, no schema-destroying migrations.

## Schema

`events(id INTEGER PRIMARY KEY, line_sha256 TEXT UNIQUE NOT NULL, received_at_utc TEXT NOT NULL, kind TEXT NOT NULL, service INTEGER, payload_json TEXT NOT NULL)`

Indexes: `(service, received_at_utc)`, `(kind, received_at_utc)`.

The complete original JSONL line is preserved as `payload_json`; no lossy aggregation, guessed units, or fabricated sensor timestamps. SHA-256 of the line prevents duplicates on repeated imports. Repeated identical lines are deduplicated (normally each line has a unique reception timestamp). Invalid JSON/schema/timestamp lines are skipped and counted; a database failure does not remove the JSONL.

## Lifecycle

The existing foreground service **continues writing JSONL**. On opening/refeshing the dashboard, the app imports all valid JSONL records into the private `cirqa_measurements.db`. Import is transactional and idempotent; dashboard currently displays its original JSONL-derived projection, not yet SQLite queries. This is a conservative first migration that avoids changing the proven BLE recorder during a field test.

## Next work

- Instrumented tests on Android emulator/device for DB create, duplicate import, truncated lines, upgrade safety, and privacy.
- Replace full-file reimport with a crash-safe cursor/checkpoint; avoid blocking UI thread for large files.
- Read dashboard metrics via indexed DB queries and add explicit day/reset segmentation.
- Validate timezone/device-day boundaries and sample quality; add scientific aggregates only after coverage tests.
- Introduce bounded retention with explicit export/consent; currently the JSONL 10 MiB cap still limits recording.
- Keep sensitive health JSONL and DB out of public Git repositories.

## Ground rules

See `docs/CIRQA_DASHBOARD_GROUND_RULES.md` and `docs/CIRQA_OVERNIGHT_FIRST_RESULTS.md`. No sleep-stage inference, no single-RR RMSSD, no addition of cumulative counters, no unvalidated calorie unit, no clinical interpretation of consumer wearable values.
