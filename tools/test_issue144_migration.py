"""Synthetic, offline regression tests for the #144 v9 data model.

These fixtures are generated in this file; they contain no device data, no raw
FIT bytes and no personal values. They verify persistence-level guarantees:
lossless additive migration, per-metric quality isolation, and that Garmin
status codes stay in status events instead of becoming physiological values.
"""

from __future__ import annotations

import sqlite3
import unittest

from verify_issue144_migration import apply_v9

SOURCE = "a" * 64
GENERATION = "b" * 64
NOW = 1_700_000_000_000
TIMESTAMP_ORIGINS = "('DIRECT', 'RECONSTRUCTED_TS16', 'RECONSTRUCTED_COMPRESSED', 'UNKNOWN')"

V8_SCHEMA = f"""
CREATE TABLE fit_sources (
    source_sha256 TEXT PRIMARY KEY NOT NULL,
    file_bytes INTEGER NOT NULL,
    relative_path TEXT NOT NULL,
    validated_at_ms INTEGER NOT NULL,
    decoder_version TEXT NOT NULL,
    profile_version TEXT NOT NULL
);
CREATE TABLE observations (
    device_id TEXT NOT NULL,
    metric TEXT NOT NULL,
    measured_at_ms INTEGER NOT NULL CHECK(measured_at_ms >= 0),
    value REAL NOT NULL,
    unit TEXT NOT NULL,
    sample_id TEXT NOT NULL DEFAULT '',
    received_at_ms INTEGER,
    PRIMARY KEY(device_id, metric, measured_at_ms, sample_id)
);
CREATE TABLE fit_decoder_generations (
    generation_id TEXT PRIMARY KEY NOT NULL,
    source_sha256 TEXT NOT NULL REFERENCES fit_sources(source_sha256),
    decoder_version TEXT NOT NULL,
    profile_version TEXT NOT NULL,
    output_digest TEXT NOT NULL,
    output_count INTEGER NOT NULL,
    completed_at_ms INTEGER NOT NULL
);
CREATE TABLE fit_generation_observations (
    generation_id TEXT NOT NULL REFERENCES fit_decoder_generations(generation_id),
    sample_id TEXT NOT NULL,
    device_id TEXT NOT NULL,
    metric TEXT NOT NULL,
    measured_at_ms INTEGER NOT NULL CHECK(measured_at_ms >= 0),
    value REAL NOT NULL,
    unit TEXT NOT NULL,
    received_at_ms INTEGER,
    record_ordinal INTEGER NOT NULL,
    global_message_number INTEGER NOT NULL,
    field_number INTEGER NOT NULL,
    quality TEXT NOT NULL CHECK(quality = 'PROFILE_VALID'),
    timestamp_origin TEXT NOT NULL CHECK(timestamp_origin IN {TIMESTAMP_ORIGINS}),
    PRIMARY KEY(generation_id, sample_id)
);
"""


class Issue144MigrationTest(unittest.TestCase):
    def setUp(self) -> None:
        self.db = sqlite3.connect(":memory:")
        self.db.execute("PRAGMA foreign_keys = ON")
        self.db.executescript(V8_SCHEMA)
        self.db.executemany(
            "INSERT INTO fit_sources VALUES (?, ?, ?, ?, ?, ?)",
            [(SOURCE, 100, f"verified_fit/{SOURCE}.fit", NOW, "1", "21.222.0")],
        )
        self.db.executemany(
            "INSERT INTO fit_decoder_generations VALUES (?, ?, ?, ?, ?, ?, ?)",
            [(GENERATION, SOURCE, "1", "21.222.0", "c" * 64, 2, NOW)],
        )
        self.rows = [
            ("device", "heart_rate", NOW, 72.0, "bpm", "sample-hr", NOW),
            ("device", "spo2", NOW, 98.0, "%", "sample-spo2", NOW),
        ]
        self.db.executemany(
            "INSERT INTO observations VALUES (?, ?, ?, ?, ?, ?, ?)", self.rows
        )
        self.db.executemany(
            """INSERT INTO fit_generation_observations VALUES
               (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)""",
            [
                (
                    GENERATION, row[5], row[0], row[1], row[2], row[3], row[4], row[6],
                    1, 20, 3, "PROFILE_VALID", "DIRECT",
                )
                for row in self.rows
            ],
        )
        self.db.execute("PRAGMA user_version = 8")
        self.db.commit()

    def tearDown(self) -> None:
        self.db.close()

    def test_v9_migration_is_additive_idempotent_and_keeps_baseline(self) -> None:
        before = list(self.db.execute("SELECT * FROM observations ORDER BY sample_id"))
        apply_v9(self.db)
        after = list(self.db.execute("SELECT * FROM observations ORDER BY sample_id"))
        self.assertEqual(before, after)
        self.assertEqual(self.db.execute("PRAGMA user_version").fetchone()[0], 9)
        self.assertEqual(self.db.execute("PRAGMA integrity_check").fetchone()[0], "ok")
        self.assertEqual(self.db.execute("PRAGMA foreign_key_check").fetchall(), [])
        quality = list(self.db.execute("SELECT metric, quality_status, timestamp_origin FROM observation_quality ORDER BY metric"))
        self.assertEqual(quality, [
            ("heart_rate", "LEGACY_PROFILE_VALID", "DIRECT"),
            ("spo2", "LEGACY_PROFILE_VALID", "DIRECT"),
        ])
        apply_v9(self.db)
        self.assertEqual(self.db.execute("SELECT COUNT(*) FROM observation_quality").fetchone()[0], 2)
        self.assertEqual(self.db.execute("PRAGMA foreign_key_check").fetchall(), [])

    def test_quality_of_one_metric_does_not_invalidate_another(self) -> None:
        apply_v9(self.db)
        self.db.execute(
            "UPDATE observation_quality SET quality_status = 'UNKNOWN' WHERE sample_id = 'sample-spo2'"
        )
        self.db.commit()
        self.assertEqual(
            self.db.execute(
                "SELECT quality_status FROM observation_quality WHERE sample_id = 'sample-hr'"
            ).fetchone()[0],
            "LEGACY_PROFILE_VALID",
        )
        self.assertEqual(
            self.db.execute(
                "SELECT value FROM observations WHERE sample_id = 'sample-hr'"
            ).fetchone()[0],
            72.0,
        )

    def test_garmin_status_codes_are_events_not_physiological_values(self) -> None:
        apply_v9(self.db)
        status_rows = [
            ("status-rr-100", "respiration", NOW, "DIRECT", -100),
            ("status-rr-200", "respiration", NOW, "DIRECT", -200),
            ("status-rr-300", "respiration", NOW, "DIRECT", -300),
            ("status-hr-0", "heart_rate", NOW, "DIRECT", 0),
            ("status-spo2-255", "spo2", NOW, "DIRECT", 255),
        ]
        self.db.executemany(
            """INSERT INTO observation_status_events
               (idempotency_key, device_id, metric, measured_at_ms, timestamp_origin,
                raw_code, status_code, measurement_mode, source_kind, source_sha256,
                decoder_version, profile_version, stored_at_ms)
               VALUES (?, 'device', ?, ?, ?, ?, 'UNKNOWN', 'UNKNOWN', 'FIT', ?, '1', '21.222.0', ?)""",
            [row + (SOURCE, NOW) for row in status_rows],
        )
        self.db.execute(
            """INSERT INTO observation_status_events
               (idempotency_key, device_id, metric, measured_at_ms, timestamp_origin,
                raw_value_text, status_code, measurement_mode, source_kind,
                decoder_version, profile_version, stored_at_ms)
               VALUES ('unknown-message', 'device', 'unknown', NULL, 'UNKNOWN',
                       'opaque', 'UNKNOWN', 'UNKNOWN', 'FIT', '1', '21.222.0', ?)""",
            (NOW,),
        )
        self.db.commit()
        values = {row[0] for row in self.db.execute("SELECT value FROM observations")}
        self.assertEqual(values, {72.0, 98.0})
        self.assertEqual(
            self.db.execute("SELECT COUNT(*) FROM observation_status_events").fetchone()[0], 6
        )
        self.assertEqual(
            self.db.execute("SELECT COUNT(*) FROM observations WHERE value < 0").fetchone()[0], 0
        )
        self.assertEqual(
            self.db.execute(
                "SELECT COUNT(*) FROM observation_status_events WHERE raw_code IN (-100, -200, -300, 0, 255)"
            ).fetchone()[0],
            5,
        )
        with self.assertRaises(sqlite3.IntegrityError):
            self.db.execute(
                """INSERT INTO observation_status_events
                   (idempotency_key, device_id, metric, measured_at_ms, timestamp_origin,
                    status_code, measurement_mode, source_kind, decoder_version,
                    profile_version, stored_at_ms)
                   VALUES ('status-rr-100', 'device', 'respiration', ?, 'DIRECT',
                           'UNKNOWN', 'UNKNOWN', 'FIT', '1', '21.222.0', ?)""",
                (NOW, NOW),
            )
        self.db.rollback()

    def test_integrity_checks_are_not_relaxed(self) -> None:
        apply_v9(self.db)
        with self.assertRaises(sqlite3.IntegrityError):
            self.db.execute(
                """INSERT INTO observation_status_events
                   (idempotency_key, device_id, metric, measured_at_ms, timestamp_origin,
                    status_code, measurement_mode, source_kind, decoder_version,
                    profile_version, stored_at_ms)
                   VALUES ('bad-time', 'device', 'respiration', ?, 'UNKNOWN',
                           'UNKNOWN', 'UNKNOWN', 'FIT', '1', '21.222.0', ?)""",
                (NOW, NOW),
            )
        self.db.rollback()
        with self.assertRaises(sqlite3.IntegrityError):
            self.db.execute(
                """INSERT INTO observation_quality
                   (device_id, metric, measured_at_ms, sample_id, quality_status,
                    measurement_mode, timestamp_origin, source_kind, decoder_version,
                    profile_version, stored_at_ms)
                   VALUES ('device', 'heart_rate', ?, 'unknown-sample', 'NOT_IN_CATALOG',
                           'UNKNOWN', 'DIRECT', 'FIT', '1', '21.222.0', ?)""",
                (NOW, NOW),
            )
        self.db.rollback()
        self.db.execute(
            """INSERT INTO import_runs
               (import_run_id, started_at_ms, decoder_version, profile_version, status)
               VALUES ('run-1', ?, '1', '21.222.0', 'PARTIAL')""",
            (NOW,),
        )
        with self.assertRaises(sqlite3.IntegrityError):
            self.db.execute(
                """INSERT INTO import_errors
                   (import_run_id, error_class, redacted_message, occurred_at_ms, decision)
                   VALUES ('run-1', 'SOURCE_CONFLICT', 'redacted', ?, 'NOT_A_DECISION')""",
                (NOW,),
            )
        self.db.rollback()
        self.assertEqual(self.db.execute("PRAGMA foreign_key_check").fetchall(), [])


if __name__ == "__main__":
    unittest.main()
