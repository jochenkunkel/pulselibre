#!/usr/bin/env python3
"""Reproducible, read-source SQLite proof for the #144 v8 -> v9 migration.

The source database is never modified: it is copied to a temporary file and the
same additive DDL/SELECT used by CirqaDatabase.kt is applied there. This is a
local evidence tool, not an Android CI gate and not a decoder/import simulation.

Usage: python3 tools/verify_issue144_migration.py /path/to/cirqa_measurements.db
"""

from __future__ import annotations

import argparse
import collections
import hashlib
import shutil
import sqlite3
import sys
import tempfile
from pathlib import Path

BASELINE_TABLES = {
    "observations": ("device_id", "metric", "measured_at_ms", "sample_id"),
    "fit_generation_observations": ("generation_id", "sample_id"),
    "fit_observation_provenance": ("sample_id",),
}

DDL = [
    """CREATE TABLE IF NOT EXISTS quality_status_catalog (
        status_code TEXT PRIMARY KEY NOT NULL,
        description TEXT NOT NULL,
        source_of_semantics TEXT NOT NULL
    )""",
    """CREATE TABLE IF NOT EXISTS measurement_modes (
        mode_code TEXT PRIMARY KEY NOT NULL,
        description TEXT NOT NULL,
        source_of_semantics TEXT NOT NULL
    )""",
    """INSERT OR IGNORE INTO quality_status_catalog(status_code, description, source_of_semantics)
       VALUES ('UNKNOWN', 'No confirmed quality semantics', 'SCHEMA_v9_FALLBACK'),
              ('LEGACY_PROFILE_VALID',
               'Faithful copy of the pre-v9 PROFILE_VALID classification; not a new validation',
               'MIGRATION_v9_LEGACY_COPY')""",
    """INSERT OR IGNORE INTO measurement_modes(mode_code, description, source_of_semantics)
       VALUES ('UNKNOWN', 'Measurement mode not available in the stored source', 'SCHEMA_v9_FALLBACK')""",
    """CREATE TABLE IF NOT EXISTS observation_quality (
        id INTEGER PRIMARY KEY AUTOINCREMENT,
        device_id TEXT NOT NULL,
        metric TEXT NOT NULL,
        measured_at_ms INTEGER NOT NULL CHECK(measured_at_ms >= 0),
        sample_id TEXT NOT NULL,
        quality_status TEXT NOT NULL REFERENCES quality_status_catalog(status_code) ON DELETE RESTRICT,
        measurement_mode TEXT NOT NULL REFERENCES measurement_modes(mode_code) ON DELETE RESTRICT,
        sensor_context TEXT NOT NULL DEFAULT 'UNKNOWN',
        confidence INTEGER CHECK(confidence IS NULL OR confidence BETWEEN 0 AND 100),
        raw_status_code INTEGER,
        timestamp_origin TEXT NOT NULL CHECK(timestamp_origin IN
            ('DIRECT', 'RECONSTRUCTED_TS16', 'RECONSTRUCTED_COMPRESSED', 'UNKNOWN')),
        source_kind TEXT NOT NULL,
        source_sha256 TEXT,
        record_ordinal INTEGER,
        global_message_number INTEGER,
        field_number INTEGER,
        decoder_version TEXT NOT NULL,
        profile_version TEXT NOT NULL,
        stored_at_ms INTEGER NOT NULL CHECK(stored_at_ms >= 0),
        UNIQUE(device_id, metric, measured_at_ms, sample_id),
        FOREIGN KEY(device_id, metric, measured_at_ms, sample_id)
            REFERENCES observations(device_id, metric, measured_at_ms, sample_id) ON DELETE CASCADE,
        FOREIGN KEY(source_sha256) REFERENCES fit_sources(source_sha256) ON DELETE RESTRICT
    )""",
    "CREATE INDEX IF NOT EXISTS observation_quality_by_time ON observation_quality(metric, measured_at_ms)",
    "CREATE INDEX IF NOT EXISTS observation_quality_by_source ON observation_quality(source_sha256, measured_at_ms)",
    """CREATE TABLE IF NOT EXISTS observation_status_events (
        id INTEGER PRIMARY KEY AUTOINCREMENT,
        idempotency_key TEXT NOT NULL UNIQUE,
        device_id TEXT NOT NULL,
        metric TEXT NOT NULL,
        measured_at_ms INTEGER CHECK(measured_at_ms IS NULL OR measured_at_ms >= 0),
        timestamp_origin TEXT NOT NULL CHECK(timestamp_origin IN
            ('DIRECT', 'RECONSTRUCTED_TS16', 'RECONSTRUCTED_COMPRESSED', 'UNKNOWN', 'NOT_APPLICABLE')),
        raw_code INTEGER,
        raw_value_text TEXT,
        status_code TEXT NOT NULL REFERENCES quality_status_catalog(status_code) ON DELETE RESTRICT,
        measurement_mode TEXT NOT NULL REFERENCES measurement_modes(mode_code) ON DELETE RESTRICT,
        sensor_context TEXT NOT NULL DEFAULT 'UNKNOWN',
        source_kind TEXT NOT NULL,
        source_sha256 TEXT,
        record_ordinal INTEGER,
        global_message_number INTEGER,
        field_number INTEGER,
        decoder_version TEXT NOT NULL,
        profile_version TEXT NOT NULL,
        observed_at_ms INTEGER,
        stored_at_ms INTEGER NOT NULL CHECK(stored_at_ms >= 0),
        CHECK ((measured_at_ms IS NOT NULL AND timestamp_origin IN
                    ('DIRECT', 'RECONSTRUCTED_TS16', 'RECONSTRUCTED_COMPRESSED'))
               OR (measured_at_ms IS NULL AND timestamp_origin IN ('UNKNOWN', 'NOT_APPLICABLE'))),
        FOREIGN KEY(source_sha256) REFERENCES fit_sources(source_sha256) ON DELETE RESTRICT
    )""",
    "CREATE INDEX IF NOT EXISTS observation_status_events_by_time ON observation_status_events(metric, measured_at_ms)",
    "CREATE INDEX IF NOT EXISTS observation_status_events_by_status ON observation_status_events(status_code)",
    """CREATE TABLE IF NOT EXISTS import_runs (
        import_run_id TEXT PRIMARY KEY NOT NULL,
        started_at_ms INTEGER NOT NULL,
        finished_at_ms INTEGER,
        source_sha256 TEXT,
        decoder_version TEXT NOT NULL,
        profile_version TEXT NOT NULL,
        status TEXT NOT NULL CHECK(status IN ('RUNNING', 'COMPLETED', 'PARTIAL', 'FAILED')),
        observed_count INTEGER NOT NULL DEFAULT 0,
        status_count INTEGER NOT NULL DEFAULT 0,
        error_count INTEGER NOT NULL DEFAULT 0
    )""",
    """CREATE TABLE IF NOT EXISTS import_errors (
        id INTEGER PRIMARY KEY AUTOINCREMENT,
        import_run_id TEXT NOT NULL REFERENCES import_runs(import_run_id) ON DELETE RESTRICT,
        source_sha256 TEXT,
        error_class TEXT NOT NULL,
        redacted_message TEXT NOT NULL,
        occurred_at_ms INTEGER NOT NULL,
        decision TEXT NOT NULL CHECK(decision IN ('FILE_SKIPPED', 'RUN_ABORTED'))
    )""",
    "CREATE INDEX IF NOT EXISTS import_errors_by_run ON import_errors(import_run_id)",
]

REBUILD_QUALITY = """INSERT OR IGNORE INTO observation_quality
   (device_id, metric, measured_at_ms, sample_id, quality_status, measurement_mode,
    sensor_context, confidence, raw_status_code, timestamp_origin, source_kind,
    source_sha256, record_ordinal, global_message_number, field_number,
    decoder_version, profile_version, stored_at_ms)
   SELECT fog.device_id, fog.metric, fog.measured_at_ms, fog.sample_id,
          'LEGACY_PROFILE_VALID', 'UNKNOWN', 'UNKNOWN', NULL, NULL, fog.timestamp_origin,
          'FIT', g.source_sha256, fog.record_ordinal, fog.global_message_number,
          fog.field_number, g.decoder_version, g.profile_version, g.completed_at_ms
   FROM fit_generation_observations fog
   JOIN fit_decoder_generations g ON g.generation_id = fog.generation_id
   JOIN observations o ON o.device_id = fog.device_id AND o.metric = fog.metric
     AND o.measured_at_ms = fog.measured_at_ms AND o.sample_id = fog.sample_id"""


def ordered_digest(con: sqlite3.Connection, table: str, order_by: tuple[str, ...]) -> tuple[int, str]:
    digest = hashlib.sha256()
    count = 0
    for row in con.execute(f"SELECT * FROM {table} ORDER BY {', '.join(order_by)}"):
        digest.update(repr(row).encode())
        count += 1
    return count, digest.hexdigest()


def snapshot(con: sqlite3.Connection) -> dict[str, tuple[int, str]]:
    return {
        table: ordered_digest(con, table, order_by)
        for table, order_by in BASELINE_TABLES.items()
    }


def apply_v9(con: sqlite3.Connection) -> None:
    con.execute("PRAGMA foreign_keys = ON")
    con.execute("BEGIN")
    try:
        for statement in DDL:
            con.execute(statement)
        con.execute(REBUILD_QUALITY)
        con.execute("PRAGMA user_version = 9")
        con.execute("COMMIT")
    except Exception:
        con.execute("ROLLBACK")
        raise


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("database", type=Path, help="source v8 SQLite database (read-only)")
    args = parser.parse_args()
    source = args.database.resolve()
    if not source.is_file():
        parser.error(f"no such database: {source}")

    with tempfile.TemporaryDirectory(prefix="issue144-migration-") as temp:
        target = Path(temp) / "migrated.db"
        shutil.copy2(source, target)
        con = sqlite3.connect(target)
        try:
            version = con.execute("PRAGMA user_version").fetchone()[0]
            if version != 8:
                parser.error(f"expected v8 source database, got user_version={version}")
            integrity = con.execute("PRAGMA integrity_check").fetchone()[0]
            assert integrity == "ok", integrity
            before = snapshot(con)
            apply_v9(con)
            after = snapshot(con)
            assert before == after, "baseline table contents changed during migration"

            fk_violations = con.execute("PRAGMA foreign_key_check").fetchall()
            assert not fk_violations, fk_violations
            assert con.execute("PRAGMA user_version").fetchone()[0] == 9
            quality_count = con.execute("SELECT COUNT(*) FROM observation_quality").fetchone()[0]
            observation_count = con.execute("SELECT COUNT(*) FROM observations").fetchone()[0]
            assert quality_count == observation_count, (quality_count, observation_count)
            statuses = dict(con.execute(
                "SELECT quality_status, COUNT(*) FROM observation_quality GROUP BY quality_status"
            ))
            assert statuses == {"LEGACY_PROFILE_VALID": observation_count}, statuses
            modes = dict(con.execute(
                "SELECT measurement_mode, COUNT(*) FROM observation_quality GROUP BY measurement_mode"
            ))
            assert modes == {"UNKNOWN": observation_count}, modes
            negative = con.execute("SELECT COUNT(*) FROM observations WHERE value < 0").fetchone()[0]
            for table, (count, digest) in after.items():
                print(f"UNCHANGED {table}: rows={count} sha256={digest}")
            print(f"QUALITY rows={quality_count} statuses={statuses} modes={modes}")
            print(f"NEGATIVE observation values={negative}")
            print(f"INTEGRITY {integrity}; foreign_key_check=[]; user_version=9")

            # Re-applying the same additive migration must not duplicate anything.
            apply_v9(con)
            assert snapshot(con) == after
            assert con.execute("SELECT COUNT(*) FROM observation_quality").fetchone()[0] == quality_count
            print("IDEMPOTENT second apply: no new rows, baseline digests unchanged")
        finally:
            con.close()
    return 0


if __name__ == "__main__":
    sys.exit(main())
