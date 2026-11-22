package de.pulselibre

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import org.json.JSONObject
import java.security.MessageDigest

/**
 * Canonical local SQLite store for CIRQA events.
 * New events are written here directly; obsolete JSONL recordings are no longer imported.
 * UNIQUE SHA-256 of each full line makes repeated imports idempotent.
 */
class CirqaDatabase(private val contextRef: Context) :
    SQLiteOpenHelper(contextRef, "cirqa_measurements.db", null, 8), FitObservationStore {

    override fun onConfigure(db: SQLiteDatabase) {
        super.onConfigure(db)
        db.setForeignKeyConstraintsEnabled(true)
    }

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL("""CREATE TABLE events (
            id INTEGER PRIMARY KEY AUTOINCREMENT,
            line_sha256 TEXT NOT NULL UNIQUE,
            received_at_utc TEXT NOT NULL,
            dashboard_time TEXT,
            kind TEXT NOT NULL,
            service INTEGER,
            payload_json TEXT NOT NULL
        )""")
        db.execSQL("CREATE INDEX events_service_time ON events(service, received_at_utc)")
        createDashboardIndex(db)
        db.execSQL("CREATE INDEX events_kind_time ON events(kind, received_at_utc)")
        createHistoricalSchema(db)
        createFitDeviceMetadataSchema(db)
        createRawDeviceMetadataSchema(db)
        createUnmappedFitFieldSchema(db)
        createFitGenerationSchema(db)
        createFitReviewCandidateSchema(db)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        require(oldVersion in 1..7 && newVersion == 8)
        if (oldVersion < 6) {
          if (oldVersion <= 2) {
            if (oldVersion == 1) {
            db.execSQL("ALTER TABLE events ADD COLUMN dashboard_time TEXT")
            var lastId = 0L
            while (true) {
                val batch = ArrayList<Pair<Long, String>>()
                db.rawQuery("SELECT id, received_at_utc FROM events WHERE id > ? AND kind = 'reading' ORDER BY id LIMIT 512",
                    arrayOf(lastId.toString())).use { rows ->
                    while (rows.moveToNext()) batch.add(rows.getLong(0) to rows.getString(1))
                }
                if (batch.isEmpty()) break
                for ((id, timestamp) in batch) {
                    db.execSQL(
                        "UPDATE events SET dashboard_time = ? WHERE id = ?",
                        arrayOf<Any?>(timeKey(timestamp), id)
                    )
                }
                lastId = batch.last().first
            }
            createDashboardIndex(db)
            }
            createHistoricalSchema(db)
            createFitDeviceMetadataSchema(db)
          } else if (oldVersion == 3) {
              createFitDeviceMetadataSchema(db)
          }
          if (oldVersion < 5) createRawDeviceMetadataSchema(db)
          createUnmappedFitFieldSchema(db)
        }
        createFitGenerationSchema(db)
        migrateExistingFitObservationsToGeneration(db)
        createFitReviewCandidateSchema(db)
    }

    private fun createFitGenerationSchema(db: SQLiteDatabase) {
        db.execSQL("""CREATE TABLE fit_decoder_generations (
            generation_id TEXT PRIMARY KEY NOT NULL CHECK(length(generation_id) = 64),
            source_sha256 TEXT NOT NULL REFERENCES fit_sources(source_sha256) ON DELETE RESTRICT,
            decoder_version TEXT NOT NULL,
            profile_version TEXT NOT NULL,
            output_digest TEXT NOT NULL CHECK(length(output_digest) = 64),
            output_count INTEGER NOT NULL CHECK(output_count >= 0),
            completed_at_ms INTEGER NOT NULL CHECK(completed_at_ms >= 0),
            UNIQUE(source_sha256, decoder_version, profile_version),
            UNIQUE(generation_id, source_sha256)
        )""")
        db.execSQL("""CREATE TABLE fit_generation_observations (
            generation_id TEXT NOT NULL REFERENCES fit_decoder_generations(generation_id) ON DELETE RESTRICT,
            sample_id TEXT NOT NULL,
            device_id TEXT NOT NULL,
            metric TEXT NOT NULL,
            measured_at_ms INTEGER NOT NULL CHECK(measured_at_ms >= 0),
            value REAL NOT NULL,
            unit TEXT NOT NULL,
            received_at_ms INTEGER CHECK(received_at_ms IS NULL OR received_at_ms >= 0),
            record_ordinal INTEGER NOT NULL CHECK(record_ordinal >= 0),
            global_message_number INTEGER NOT NULL CHECK(global_message_number >= 0),
            field_number INTEGER NOT NULL CHECK(field_number >= 0),
            quality TEXT NOT NULL CHECK(quality = 'PROFILE_VALID'),
            timestamp_origin TEXT NOT NULL CHECK(timestamp_origin IN
                ('DIRECT', 'RECONSTRUCTED_TS16', 'RECONSTRUCTED_COMPRESSED', 'UNKNOWN')),
            PRIMARY KEY(generation_id, sample_id)
        )""")
        db.execSQL("CREATE INDEX fit_generation_observations_by_time ON fit_generation_observations(measured_at_ms)")
        db.execSQL("""CREATE TABLE fit_active_generations (
            source_sha256 TEXT PRIMARY KEY NOT NULL REFERENCES fit_sources(source_sha256) ON DELETE RESTRICT,
            generation_id TEXT NOT NULL UNIQUE,
            FOREIGN KEY(generation_id, source_sha256)
                REFERENCES fit_decoder_generations(generation_id, source_sha256) ON DELETE RESTRICT
        )""")
    }

    private fun createFitReviewCandidateSchema(db: SQLiteDatabase) {
        db.execSQL("""CREATE TABLE IF NOT EXISTS fit_review_candidates (
            sample_id TEXT PRIMARY KEY NOT NULL,
            device_id TEXT NOT NULL,
            metric TEXT NOT NULL,
            measured_at_ms INTEGER CHECK(measured_at_ms IS NULL OR measured_at_ms >= 0),
            value REAL NOT NULL,
            unit TEXT NOT NULL,
            source_sha256 TEXT NOT NULL REFERENCES fit_sources(source_sha256) ON DELETE RESTRICT,
            record_ordinal INTEGER NOT NULL CHECK(record_ordinal > 0),
            global_message_number INTEGER NOT NULL CHECK(global_message_number >= 0),
            field_number INTEGER NOT NULL CHECK(field_number >= 0),
            quality TEXT NOT NULL CHECK(quality = 'UNTRUSTED_PROFILE_CANDIDATE'),
            timestamp_origin TEXT NOT NULL CHECK(timestamp_origin IN
                ('DIRECT', 'RECONSTRUCTED_TS16', 'RECONSTRUCTED_COMPRESSED', 'UNKNOWN')),
            decoder_version TEXT NOT NULL,
            UNIQUE(source_sha256, record_ordinal, global_message_number, field_number)
        )""")
        db.execSQL("CREATE INDEX IF NOT EXISTS fit_review_candidates_by_time ON fit_review_candidates(metric, measured_at_ms)")
    }

    /**
     * Preserve the existing FIT rows and create their first immutable generation.
     * Their historical timestamp derivation is unknowable, so origin is UNKNOWN.
     * SQLiteOpenHelper wraps onUpgrade in a transaction; any conflict aborts all DDL/data.
     */
    private fun migrateExistingFitObservationsToGeneration(db: SQLiteDatabase) {
        val hasProfileVersion = hasColumn(db, "fit_sources", "profile_version")
        val hasValidatedAt = hasColumn(db, "fit_sources", "validated_at_ms")
        val profileExpression = if (hasProfileVersion) "profile_version" else "'legacy-profile-unknown-v1'"
        val validatedAtExpression = if (hasValidatedAt) "validated_at_ms" else "0"
        db.rawQuery(
            """SELECT source_sha256, $profileExpression, $validatedAtExpression
               FROM fit_sources
               WHERE NOT EXISTS (
                   SELECT 1 FROM fit_active_generations a
                   WHERE a.source_sha256 = fit_sources.source_sha256
               )
               ORDER BY source_sha256""", null
        ).use { sources ->
            while (sources.moveToNext()) {
                val sourceSha = sources.getString(0)
                val profileVersion = sources.getString(1)
                val completedAt = sources.getLong(2)
                // Historical decoder provenance cannot be inferred after the fact.
                val legacyDecoderVersion = "legacy-unknown-v1"
                val generationId = FitDecoderGeneration.id(
                    sourceSha, legacyDecoderVersion, profileVersion
                )
                val values = mutableListOf<StoredFitGenerationObservation>()
                if (hasTable(db, "fit_observation_provenance") && hasTable(db, "observations")) {
                    db.rawQuery(
                        """SELECT p.sample_id, p.device_id, p.metric, p.measured_at_ms, o.value,
                                  o.unit, o.received_at_ms, p.record_ordinal, p.global_message_number,
                                  p.field_number, p.quality
                           FROM fit_observation_provenance p
                           JOIN observations o ON o.device_id = p.device_id AND o.metric = p.metric
                             AND o.measured_at_ms = p.measured_at_ms AND o.sample_id = p.sample_id
                           WHERE p.source_sha256 = ?
                           ORDER BY p.sample_id""",
                        arrayOf(sourceSha)
                    ).use { cursor ->
                        while (cursor.moveToNext()) {
                            values += StoredFitGenerationObservation(
                                generationId = generationId,
                                sampleId = cursor.getString(0),
                                deviceId = cursor.getString(1),
                                metric = cursor.getString(2),
                                measuredAtEpochMillis = cursor.getLong(3),
                                value = cursor.getDouble(4),
                                unit = cursor.getString(5),
                                receivedAtEpochMillis = if (cursor.isNull(6)) null else cursor.getLong(6),
                                recordOrdinal = cursor.getInt(7),
                                globalMessageNumber = cursor.getInt(8),
                                fieldNumber = cursor.getInt(9),
                                quality = cursor.getString(10),
                                timestampOrigin = "UNKNOWN"
                            )
                        }
                    }
                }
                db.execSQL(
                    """INSERT INTO fit_decoder_generations
                       (generation_id, source_sha256, decoder_version, profile_version,
                        output_digest, output_count, completed_at_ms)
                       VALUES(?, ?, ?, ?, ?, ?, ?)""",
                    arrayOf<Any?>(
                        generationId, sourceSha, legacyDecoderVersion, profileVersion,
                        FitDecoderGeneration.outputDigest(values), values.size, completedAt
                    )
                )
                val insert = db.compileStatement(
                    """INSERT INTO fit_generation_observations
                       (generation_id, sample_id, device_id, metric, measured_at_ms, value, unit,
                        received_at_ms, record_ordinal, global_message_number, field_number, quality,
                        timestamp_origin)
                       VALUES(?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, 'UNKNOWN')"""
                )
                values.forEach { row ->
                    insert.clearBindings()
                    insert.bindString(1, generationId)
                    insert.bindString(2, row.sampleId)
                    insert.bindString(3, row.deviceId)
                    insert.bindString(4, row.metric)
                    insert.bindLong(5, row.measuredAtEpochMillis)
                    insert.bindDouble(6, row.value)
                    insert.bindString(7, row.unit)
                    if (row.receivedAtEpochMillis == null) insert.bindNull(8)
                    else insert.bindLong(8, row.receivedAtEpochMillis)
                    insert.bindLong(9, row.recordOrdinal.toLong())
                    insert.bindLong(10, row.globalMessageNumber.toLong())
                    insert.bindLong(11, row.fieldNumber.toLong())
                    insert.bindString(12, row.quality)
                    insert.executeInsert()
                }
                db.execSQL(
                    "INSERT INTO fit_active_generations(source_sha256, generation_id) VALUES(?, ?)",
                    arrayOf(sourceSha, generationId)
                )
            }
        }
    }

    private fun hasColumn(db: SQLiteDatabase, table: String, column: String): Boolean =
        db.rawQuery("PRAGMA table_info($table)", null).use { cursor ->
            var found = false
            while (cursor.moveToNext()) if (cursor.getString(1) == column) found = true
            found
        }

    private fun hasTable(db: SQLiteDatabase, table: String): Boolean =
        db.rawQuery(
            "SELECT 1 FROM sqlite_master WHERE type = 'table' AND name = ?",
            arrayOf(table)
        ).use { it.moveToFirst() }

    private fun ensureMissingFitGenerations(db: SQLiteDatabase) {
        db.beginTransaction()
        try {
            migrateExistingFitObservationsToGeneration(db)
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
    }

    private fun createHistoricalSchema(db: SQLiteDatabase) {
        db.execSQL("""CREATE TABLE storage_migrations (
            migration_key TEXT PRIMARY KEY NOT NULL,
            source_readings INTEGER NOT NULL,
            source_cursors INTEGER NOT NULL,
            source_fit_sources INTEGER NOT NULL,
            source_fit_provenance INTEGER NOT NULL,
            completed_at_ms INTEGER NOT NULL
        )""")
        db.execSQL("""CREATE TABLE observations (
            device_id TEXT NOT NULL,
            metric TEXT NOT NULL,
            measured_at_ms INTEGER NOT NULL CHECK(measured_at_ms >= 0),
            value REAL NOT NULL,
            unit TEXT NOT NULL,
            sample_id TEXT NOT NULL DEFAULT '',
            received_at_ms INTEGER,
            PRIMARY KEY(device_id, metric, measured_at_ms, sample_id)
        )""")
        db.execSQL("CREATE INDEX observations_by_time ON observations(measured_at_ms)")
        db.execSQL("""CREATE TABLE sync_cursors (
            device_id TEXT PRIMARY KEY NOT NULL,
            last_committed_ms INTEGER NOT NULL CHECK(last_committed_ms >= 0)
        )""")
        db.execSQL("""CREATE TABLE fit_sources (
            source_sha256 TEXT PRIMARY KEY NOT NULL CHECK(length(source_sha256) = 64),
            file_bytes INTEGER NOT NULL CHECK(file_bytes > 0),
            relative_path TEXT NOT NULL,
            validated_at_ms INTEGER NOT NULL CHECK(validated_at_ms >= 0),
            decoder_version TEXT NOT NULL,
            profile_version TEXT NOT NULL
        )""")
        db.execSQL("""CREATE TABLE fit_observation_provenance (
            sample_id TEXT PRIMARY KEY NOT NULL,
            device_id TEXT NOT NULL,
            metric TEXT NOT NULL,
            measured_at_ms INTEGER NOT NULL CHECK(measured_at_ms >= 0),
            source_sha256 TEXT NOT NULL REFERENCES fit_sources(source_sha256) ON DELETE RESTRICT,
            record_ordinal INTEGER NOT NULL CHECK(record_ordinal >= 0),
            global_message_number INTEGER NOT NULL CHECK(global_message_number >= 0),
            field_number INTEGER NOT NULL CHECK(field_number >= 0),
            quality TEXT NOT NULL CHECK(quality = 'PROFILE_VALID'),
            decoder_version TEXT NOT NULL,
            FOREIGN KEY(device_id, metric, measured_at_ms, sample_id)
                REFERENCES observations(device_id, metric, measured_at_ms, sample_id) ON DELETE CASCADE
        )""")
        db.execSQL("CREATE INDEX fit_provenance_by_source ON fit_observation_provenance(source_sha256, record_ordinal)")
        db.execSQL("CREATE INDEX fit_provenance_by_metric_time ON fit_observation_provenance(metric, measured_at_ms)")
    }

    private fun createFitDeviceMetadataSchema(db: SQLiteDatabase) {
        db.execSQL("""CREATE TABLE fit_device_metadata (
            source_sha256 TEXT NOT NULL REFERENCES fit_sources(source_sha256) ON DELETE RESTRICT,
            record_ordinal INTEGER NOT NULL CHECK(record_ordinal > 0),
            observed_at_ms INTEGER CHECK(observed_at_ms IS NULL OR observed_at_ms >= 0),
            device_type_code INTEGER CHECK(device_type_code IS NULL OR device_type_code BETWEEN 0 AND 254),
            manufacturer_code INTEGER CHECK(manufacturer_code IS NULL OR manufacturer_code BETWEEN 0 AND 65534),
            product_code INTEGER CHECK(product_code IS NULL OR product_code BETWEEN 0 AND 65534),
            software_version REAL CHECK(software_version IS NULL OR software_version BETWEEN 0 AND 655.34),
            battery_voltage_volts REAL CHECK(battery_voltage_volts IS NULL OR battery_voltage_volts BETWEEN 0 AND 20),
            battery_status_code INTEGER CHECK(battery_status_code IS NULL OR battery_status_code BETWEEN 0 AND 254),
            battery_level_percent INTEGER CHECK(battery_level_percent IS NULL OR battery_level_percent BETWEEN 0 AND 100),
            PRIMARY KEY(source_sha256, record_ordinal)
        )""")
        db.execSQL("CREATE INDEX fit_device_metadata_by_time ON fit_device_metadata(observed_at_ms)")
    }

    private fun createRawDeviceMetadataSchema(db: SQLiteDatabase) {
        db.execSQL("""CREATE TABLE raw_device_metadata (
            source_sha256 TEXT PRIMARY KEY NOT NULL CHECK(length(source_sha256) = 64),
            file_bytes INTEGER NOT NULL CHECK(file_bytes > 0),
            verified_at_ms INTEGER NOT NULL CHECK(verified_at_ms >= 0),
            parser_version TEXT NOT NULL CHECK(length(parser_version) BETWEEN 1 AND 32),
            software_version TEXT CHECK(software_version IS NULL OR length(software_version) BETWEEN 1 AND 128),
            mass_storage_mode INTEGER CHECK(mass_storage_mode IS NULL OR mass_storage_mode IN (0, 1)),
            transfer_direction_count INTEGER NOT NULL CHECK(transfer_direction_count BETWEEN 0 AND 10000)
        )""")
        db.execSQL("CREATE INDEX raw_device_metadata_by_verified_time ON raw_device_metadata(verified_at_ms)")
    }

    private fun createUnmappedFitFieldSchema(db: SQLiteDatabase) {
        db.execSQL("""CREATE TABLE fit_unmapped_fields (
            source_sha256 TEXT NOT NULL REFERENCES fit_sources(source_sha256) ON DELETE RESTRICT,
            global_message_number INTEGER NOT NULL CHECK(global_message_number BETWEEN 0 AND 65535),
            field_kind TEXT NOT NULL CHECK(field_kind IN ('STANDARD', 'DEVELOPER')),
            field_number INTEGER NOT NULL CHECK(field_number BETWEEN 0 AND 255),
            base_type_code INTEGER NOT NULL CHECK(base_type_code BETWEEN -1 AND 255),
            field_size INTEGER NOT NULL CHECK(field_size BETWEEN 1 AND 255),
            developer_data_index INTEGER NOT NULL CHECK(developer_data_index BETWEEN -1 AND 255),
            classification TEXT NOT NULL CHECK(classification IN (
                'MESSAGE_NOT_DECODED_BY_APP', 'FIELD_NOT_DECODED_BY_APP',
                'KNOWN_MAPPING_INCOMPATIBLE', 'DEVELOPER_FIELD_NOT_DECODED_BY_APP'
            )),
            definition_count INTEGER NOT NULL CHECK(definition_count BETWEEN 0 AND 1000000),
            observed_message_count INTEGER NOT NULL CHECK(observed_message_count BETWEEN 0 AND 1000000),
            valid_element_count INTEGER CHECK(valid_element_count IS NULL OR valid_element_count BETWEEN 0 AND 1000000),
            invalid_element_count INTEGER CHECK(invalid_element_count IS NULL OR invalid_element_count BETWEEN 0 AND 1000000),
            decoder_version TEXT NOT NULL CHECK(length(decoder_version) BETWEEN 1 AND 32),
            profile_version TEXT NOT NULL CHECK(length(profile_version) BETWEEN 1 AND 32),
            review_status TEXT NOT NULL DEFAULT 'UNREVIEWED'
                CHECK(review_status IN ('UNREVIEWED', 'INVESTIGATING', 'SUPPORTED', 'IGNORED')),
            PRIMARY KEY(source_sha256, global_message_number, field_kind, field_number,
                        base_type_code, field_size, developer_data_index, classification)
        )""")
        db.execSQL("CREATE INDEX fit_unmapped_fields_by_status ON fit_unmapped_fields(review_status)")
    }

    private fun createDashboardIndex(db: SQLiteDatabase) {
        db.execSQL("CREATE INDEX events_dashboard_time ON events(service, dashboard_time DESC, id ASC) WHERE kind = 'reading'")
    }

    fun latestEventId(): Long = readableDatabase.rawQuery("SELECT COALESCE(MAX(id), 0) FROM events", null).use {
        it.moveToFirst()
        it.getLong(0)
    }

    override fun onOpen(db: SQLiteDatabase) {
        super.onOpen(db)
        // Explicit user decision: discard the former JSONL night recording.
        // SQLite events are preserved. Retry on a later open if deletion fails.
        // One stat() call when nothing is left; with the shared poll helper this runs once per open, not once per second.
        LegacyNightRecording.remove(contextRef.filesDir)
    }

    /**
     * Idempotently copies legacy historical rows into this canonical store and
     * verifies exact row equality. The source SQLite file remains untouched.
     */
    @Synchronized fun ensureHistoricalStoreCopied() {
        LocalReadingStore(contextRef).use { sourceStore ->
            val source = sourceStore.readableDatabase
            val destination = writableDatabase
            val sourceCounts = listOf(
                rowCount(source, "readings"),
                rowCount(source, "sync_cursors"),
                rowCount(source, "fit_sources"),
                rowCount(source, "fit_observation_provenance")
            )
            val priorCopy = destination.rawQuery(
                """SELECT source_readings, source_cursors, source_fit_sources, source_fit_provenance
                   FROM storage_migrations WHERE migration_key = 'historical-v1-v2-copy'""", null
            ).use { row ->
                if (!row.moveToFirst()) null else (0..3).map { row.getLong(it) }
            }
            if (priorCopy == sourceCounts) {
                ensureMissingFitGenerations(destination)
                return
            }

            source.beginTransactionNonExclusive()
            try {
                destination.beginTransaction()
                try {
                val readingInsert = destination.compileStatement(
                    """INSERT OR IGNORE INTO observations
                       (device_id, metric, measured_at_ms, value, unit, sample_id, received_at_ms)
                       VALUES(?, ?, ?, ?, ?, ?, ?)"""
                )
                source.rawQuery(
                    """SELECT device_id, metric, measured_at_ms, value, unit, sample_id, received_at_ms
                       FROM readings ORDER BY device_id, metric, measured_at_ms, sample_id""", null
                ).use { rows ->
                    while (rows.moveToNext()) {
                        readingInsert.clearBindings()
                        readingInsert.bindString(1, rows.getString(0))
                        readingInsert.bindString(2, rows.getString(1))
                        readingInsert.bindLong(3, rows.getLong(2))
                        readingInsert.bindDouble(4, rows.getDouble(3))
                        readingInsert.bindString(5, rows.getString(4))
                        readingInsert.bindString(6, rows.getString(5))
                        if (rows.isNull(6)) readingInsert.bindNull(7) else readingInsert.bindLong(7, rows.getLong(6))
                        readingInsert.executeInsert()
                        destination.rawQuery(
                            """SELECT value, unit, received_at_ms FROM observations
                               WHERE device_id = ? AND metric = ? AND measured_at_ms = ? AND sample_id = ?""",
                            arrayOf(rows.getString(0), rows.getString(1), rows.getLong(2).toString(), rows.getString(5))
                        ).use { copied ->
                            check(copied.moveToFirst() &&
                                copied.getDouble(0).toBits() == rows.getDouble(3).toBits() &&
                                copied.getString(1) == rows.getString(4) &&
                                copied.isNull(2) == rows.isNull(6) &&
                                (rows.isNull(6) || copied.getLong(2) == rows.getLong(6))) {
                                "Historical reading key conflicts with a canonical row"
                            }
                        }
                    }
                }

                val cursorInsert = destination.compileStatement(
                    """INSERT INTO sync_cursors(device_id, last_committed_ms) VALUES(?, ?)
                       ON CONFLICT(device_id) DO UPDATE SET
                       last_committed_ms = MAX(last_committed_ms, excluded.last_committed_ms)"""
                )
                source.rawQuery("SELECT device_id, last_committed_ms FROM sync_cursors ORDER BY device_id", null)
                    .use { rows ->
                        while (rows.moveToNext()) {
                            cursorInsert.clearBindings()
                            cursorInsert.bindString(1, rows.getString(0))
                            cursorInsert.bindLong(2, rows.getLong(1))
                            cursorInsert.executeInsert()
                            destination.rawQuery(
                                "SELECT last_committed_ms FROM sync_cursors WHERE device_id = ?",
                                arrayOf(rows.getString(0))
                            ).use { copied ->
                                check(copied.moveToFirst() && copied.getLong(0) >= rows.getLong(1)) {
                                    "Canonical cursor is behind the historical cursor"
                                }
                            }
                        }
                    }

                val sourceInsert = destination.compileStatement(
                    """INSERT OR IGNORE INTO fit_sources
                       (source_sha256, file_bytes, relative_path, validated_at_ms, decoder_version, profile_version)
                       VALUES(?, ?, ?, ?, ?, ?)"""
                )
                source.rawQuery(
                    """SELECT source_sha256, file_bytes, relative_path, validated_at_ms, decoder_version, profile_version
                       FROM fit_sources ORDER BY source_sha256""", null
                ).use { rows ->
                    while (rows.moveToNext()) {
                        sourceInsert.clearBindings()
                        sourceInsert.bindString(1, rows.getString(0))
                        sourceInsert.bindLong(2, rows.getLong(1))
                        sourceInsert.bindString(3, rows.getString(2))
                        sourceInsert.bindLong(4, rows.getLong(3))
                        sourceInsert.bindString(5, rows.getString(4))
                        sourceInsert.bindString(6, rows.getString(5))
                        sourceInsert.executeInsert()
                        destination.rawQuery(
                            """SELECT file_bytes, relative_path, validated_at_ms, decoder_version, profile_version
                               FROM fit_sources WHERE source_sha256 = ?""",
                            arrayOf(rows.getString(0))
                        ).use { copied ->
                            check(copied.moveToFirst() &&
                                copied.getLong(0) == rows.getLong(1) &&
                                copied.getString(1) == rows.getString(2) &&
                                copied.getLong(2) == rows.getLong(3) &&
                                copied.getString(3) == rows.getString(4) &&
                                copied.getString(4) == rows.getString(5)) {
                                "Historical FIT source conflicts with a canonical source"
                            }
                        }
                    }
                }

                val provenanceInsert = destination.compileStatement(
                    """INSERT OR IGNORE INTO fit_observation_provenance
                       (sample_id, device_id, metric, measured_at_ms, source_sha256, record_ordinal,
                        global_message_number, field_number, quality, decoder_version)
                       VALUES(?, ?, ?, ?, ?, ?, ?, ?, ?, ?)"""
                )
                source.rawQuery(
                    """SELECT sample_id, device_id, metric, measured_at_ms, source_sha256, record_ordinal,
                              global_message_number, field_number, quality, decoder_version
                       FROM fit_observation_provenance ORDER BY sample_id""", null
                ).use { rows ->
                    while (rows.moveToNext()) {
                        provenanceInsert.clearBindings()
                        for (index in 0..9) {
                            when (index) {
                                3, 5, 6, 7 -> provenanceInsert.bindLong(index + 1, rows.getLong(index))
                                else -> provenanceInsert.bindString(index + 1, rows.getString(index))
                            }
                        }
                        provenanceInsert.executeInsert()
                        destination.rawQuery(
                            """SELECT device_id, metric, measured_at_ms, source_sha256, record_ordinal,
                                      global_message_number, field_number, quality, decoder_version
                               FROM fit_observation_provenance WHERE sample_id = ?""",
                            arrayOf(rows.getString(0))
                        ).use { copied ->
                            check(copied.moveToFirst() &&
                                (0..8).all { column ->
                                    when (column) {
                                        2, 4, 5, 6 -> copied.getLong(column) == rows.getLong(column + 1)
                                        else -> copied.getString(column) == rows.getString(column + 1)
                                    }
                                }) { "Historical FIT provenance conflicts with a canonical row" }
                        }
                    }
                }

                checkCountsMatch(source, destination, "readings", "observations")
                checkCountsMatch(source, destination, "sync_cursors", "sync_cursors")
                checkCountsMatch(source, destination, "fit_sources", "fit_sources")
                checkCountsMatch(
                    source, destination, "fit_observation_provenance", "fit_observation_provenance"
                )
                destination.execSQL(
                    """INSERT OR REPLACE INTO storage_migrations
                       (migration_key, source_readings, source_cursors, source_fit_sources,
                        source_fit_provenance, completed_at_ms)
                       VALUES('historical-v1-v2-copy', ?, ?, ?, ?, ?)""",
                    arrayOf(
                        sourceCounts[0], sourceCounts[1], sourceCounts[2], sourceCounts[3],
                        System.currentTimeMillis()
                    )
                )
                migrateExistingFitObservationsToGeneration(destination)
                destination.setTransactionSuccessful()
                } finally {
                    destination.endTransaction()
                }
                source.setTransactionSuccessful()
            } finally {
                source.endTransaction()
            }
        }
    }

    private fun checkCountsMatch(source: SQLiteDatabase, destination: SQLiteDatabase, sourceTable: String, targetTable: String) {
        val sourceCount = rowCount(source, sourceTable)
        val targetCount = destination.rawQuery("SELECT COUNT(*) FROM $targetTable", null)
            .use { it.moveToFirst(); it.getLong(0) }
        check(sourceCount == targetCount) {
            "Historical migration verification failed for $sourceTable: source=$sourceCount target=$targetCount"
        }
    }

    private fun rowCount(db: SQLiteDatabase, table: String): Long =
        db.rawQuery("SELECT COUNT(*) FROM $table", null).use { it.moveToFirst(); it.getLong(0) }

    override fun commitFitFile(
        sourceSha256: String,
        fileBytes: Long,
        relativePath: String,
        validatedAtEpochMillis: Long,
        decoderVersion: String,
        profileVersion: String,
        observations: List<FitHistoricalObservations.Observation>,
        deviceMetadata: List<FitHistoricalObservations.DeviceMetadata>,
        unmappedFields: List<FitFieldInventoryRecord>,
        deviceKey: String,
        reviewCandidates: List<FitHistoricalObservations.ReviewCandidate>
    ): Int {
        ensureHistoricalStoreCopied()
        return FitObservationCommitter.commit(
            writableDatabase, "observations", sourceSha256, fileBytes, relativePath,
            validatedAtEpochMillis, decoderVersion, profileVersion, observations, deviceMetadata,
            unmappedFields, deviceKey, reviewCandidates
        )
    }

    /** Returns raw FIT-derived values separately from trusted historical observations. */
    fun readUntrustedFitCandidates(metric: String? = null): List<StoredFitReviewCandidate> {
        require(metric == null || metric in setOf("spo2", "stress", "respiration"))
        val result = mutableListOf<StoredFitReviewCandidate>()
        val sql = """SELECT sample_id, device_id, metric, measured_at_ms, value, unit, source_sha256,
                            record_ordinal, global_message_number, field_number, quality,
                            timestamp_origin, decoder_version
                     FROM fit_review_candidates""" +
            if (metric == null) " ORDER BY measured_at_ms IS NULL, measured_at_ms, sample_id"
            else " WHERE metric = ? ORDER BY measured_at_ms IS NULL, measured_at_ms, sample_id"
        readableDatabase.rawQuery(sql, if (metric == null) null else arrayOf(metric)).use { rows ->
            while (rows.moveToNext()) result += StoredFitReviewCandidate(
                sampleId = rows.getString(0),
                deviceId = rows.getString(1),
                metric = rows.getString(2),
                measuredAtEpochMillis = if (rows.isNull(3)) null else rows.getLong(3),
                value = rows.getDouble(4),
                unit = rows.getString(5),
                sourceSha256 = rows.getString(6),
                recordOrdinal = rows.getInt(7),
                globalMessageNumber = rows.getInt(8),
                fieldNumber = rows.getInt(9),
                quality = rows.getString(10),
                timestampOrigin = rows.getString(11),
                decoderVersion = rows.getString(12)
            )
        }
        return result
    }

    /**
     * Re-decode retained, verified FIT bytes with the current allowlisted
     * decoder and persist a new immutable generation. The original must already
     * be present in the canonical source registry; this never reads files or
     * performs device/BLE I/O.
     */
    fun replayVerifiedFitSource(
        sourceBytes: ByteArray,
        replayedAtEpochMillis: Long,
        deviceKey: String
    ): String {
        require(sourceBytes.isNotEmpty())
        val sourceSha256 = MessageDigest.getInstance("SHA-256").digest(sourceBytes)
            .joinToString("") { "%02x".format(it.toInt() and 0xff) }
        check(FitFileHeader.inspectVerified(sourceBytes) != null) { "FIT replay source failed envelope/CRC validation" }
        val decoded = FitHistoricalObservations.decode(sourceBytes)
            ?: error("FIT replay decoder rejected the verified source")
        return storeFitDecoderReplay(
            sourceSha256 = sourceSha256,
            decoderVersion = FitHistoricalObservations.DECODER_VERSION,
            profileVersion = FitHistoricalObservations.PROFILE_VERSION,
            replayedAtEpochMillis = replayedAtEpochMillis,
            observations = decoded.observations,
            deviceKey = deviceKey
        )
    }

    /**
     * Store already-decoded output for a specific decoder/profile identity.
     * This never changes fit_sources, legacy readings, or an existing active
     * pointer; the first normal import must establish that pointer.
     */
    internal fun storeFitDecoderReplay(
        sourceSha256: String,
        decoderVersion: String,
        profileVersion: String,
        replayedAtEpochMillis: Long,
        observations: List<FitHistoricalObservations.Observation>,
        deviceKey: String
    ): String {
        require(sourceSha256.matches(Regex("[0-9a-f]{64}")))
        require(decoderVersion.isNotBlank() && profileVersion.isNotBlank())
        require(replayedAtEpochMillis >= 0L && deviceKey.isNotBlank())
        require(observations.all {
                it.sourceFileSha256 == sourceSha256 &&
                it.quality == FitHistoricalObservations.Quality.PROFILE_VALID &&
                it.timestampEpochMillis != null && it.timestampEpochMillis >= 0L && it.metric.unit == it.unit &&
                it.value.isFinite() && it.idempotencyKey.isNotBlank() &&
                it.recordOrdinal >= 0 && it.globalMessageNumber >= 0 && it.fieldNumber >= 0
        }) { "FIT replay output does not match the validated source" }
        require(observations.map { it.idempotencyKey }.distinct().size == observations.size) {
            "Duplicate FIT idempotency key in replay output"
        }
        ensureHistoricalStoreCopied()
        val db = writableDatabase
        db.beginTransaction()
        try {
            check(db.rawQuery(
                "SELECT 1 FROM fit_sources WHERE source_sha256 = ?",
                arrayOf(sourceSha256)
            ).use { it.moveToFirst() }) { "FIT replay source is not committed in the canonical store" }
            check(db.rawQuery(
                "SELECT 1 FROM fit_active_generations WHERE source_sha256 = ?",
                arrayOf(sourceSha256)
            ).use { it.moveToFirst() }) {
                "FIT replay requires an existing active generation; use the normal import path for first import"
            }
            val sourceDevices = mutableSetOf<String>()
            db.rawQuery(
                """SELECT DISTINCT g.device_id
                   FROM fit_generation_observations g
                   JOIN fit_active_generations a ON a.generation_id = g.generation_id
                   WHERE a.source_sha256 = ?""",
                arrayOf(sourceSha256)
            ).use { rows -> while (rows.moveToNext()) sourceDevices += rows.getString(0) }
            check(sourceDevices.isEmpty() || sourceDevices == setOf(deviceKey)) {
                "FIT replay device mapping differs from the active source generation"
            }
            val records = observations.map {
                RawExportRecord(
                    deviceId = deviceKey,
                    metric = it.metric.name.lowercase(),
                    measuredAtEpochMillis = requireNotNull(it.timestampEpochMillis),
                    value = it.value,
                    unit = it.unit,
                    sampleId = it.idempotencyKey,
                    receivedAtEpochMillis = replayedAtEpochMillis
                )
            }
            val generationId = FitDecoderGeneration.id(sourceSha256, decoderVersion, profileVersion)
            FitObservationCommitter.persistDecoderGeneration(
                db = db,
                sourceSha256 = sourceSha256,
                decoderVersion = decoderVersion,
                profileVersion = profileVersion,
                completedAtEpochMillis = replayedAtEpochMillis,
                observations = observations,
                records = records
            )
            check(db.rawQuery(
                "SELECT 1 FROM fit_decoder_generations WHERE generation_id = ?",
                arrayOf(generationId)
            ).use { it.moveToFirst() }) { "FIT replay generation was not persisted" }
            db.setTransactionSuccessful()
            return generationId
        } finally {
            db.endTransaction()
        }
    }

    /** Explicitly switch the History projection after validating the full immutable generation. */
    fun activateFitDecoderGeneration(sourceSha256: String, generationId: String) {
        require(sourceSha256.matches(Regex("[0-9a-f]{64}")))
        require(generationId.matches(Regex("[0-9a-f]{64}")))
        ensureHistoricalStoreCopied()
        val db = writableDatabase
        db.beginTransaction()
        try {
            val header = db.rawQuery(
                """SELECT source_sha256, decoder_version, profile_version, output_digest, output_count
                   FROM fit_decoder_generations WHERE generation_id = ?""",
                arrayOf(generationId)
            ).use { row ->
                check(row.moveToFirst() && row.getString(0) == sourceSha256) {
                    "FIT generation is missing or belongs to another source"
                }
                check(generationId == FitDecoderGeneration.id(sourceSha256, row.getString(1), row.getString(2))) {
                    "FIT generation identity does not match its source and decoder"
                }
                listOf(row.getString(3), row.getLong(4).toString())
            }
            val generationRows = mutableListOf<StoredFitGenerationObservation>()
            db.rawQuery(
                """SELECT generation_id, sample_id, device_id, metric, measured_at_ms, value,
                          unit, received_at_ms, record_ordinal, global_message_number,
                          field_number, quality, timestamp_origin
                   FROM fit_generation_observations WHERE generation_id = ? ORDER BY sample_id""",
                arrayOf(generationId)
            ).use { rows ->
                while (rows.moveToNext()) generationRows += StoredFitGenerationObservation(
                    generationId = rows.getString(0),
                    sampleId = rows.getString(1),
                    deviceId = rows.getString(2),
                    metric = rows.getString(3),
                    measuredAtEpochMillis = rows.getLong(4),
                    value = rows.getDouble(5),
                    unit = rows.getString(6),
                    receivedAtEpochMillis = if (rows.isNull(7)) null else rows.getLong(7),
                    recordOrdinal = rows.getInt(8),
                    globalMessageNumber = rows.getInt(9),
                    fieldNumber = rows.getInt(10),
                    quality = rows.getString(11),
                    timestampOrigin = rows.getString(12)
                )
            }
            check(generationRows.size.toLong() == header[1].toLong() &&
                FitDecoderGeneration.outputDigest(generationRows) == header[0]) {
                "FIT generation is incomplete or its immutable output digest is invalid"
            }
            db.execSQL(
                """INSERT INTO fit_active_generations(source_sha256, generation_id)
                   VALUES(?, ?)
                   ON CONFLICT(source_sha256) DO UPDATE SET generation_id = excluded.generation_id""",
                arrayOf(sourceSha256, generationId)
            )
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
    }

    fun storedFitUnmappedFields(): List<StoredFitUnmappedField> =
        readableDatabase.rawQuery(
            """SELECT source_sha256, global_message_number, field_kind, field_number,
                      base_type_code, field_size, developer_data_index, classification,
                      definition_count, observed_message_count, valid_element_count,
                      invalid_element_count, decoder_version, profile_version, review_status
               FROM fit_unmapped_fields
               ORDER BY source_sha256, global_message_number, field_kind, field_number""",
            null
        ).use { rows ->
            buildList {
                while (rows.moveToNext()) add(StoredFitUnmappedField(
                    sourceSha256 = rows.getString(0),
                    globalMessageNumber = rows.getInt(1),
                    kind = StoredFitFieldKind.valueOf(rows.getString(2)),
                    fieldNumber = rows.getInt(3),
                    baseTypeCode = rows.getInt(4).takeUnless { it == -1 },
                    fieldSize = rows.getInt(5),
                    developerDataIndex = rows.getInt(6).takeUnless { it == -1 },
                    classification = rows.getString(7),
                    definitionCount = rows.getLong(8),
                    observedMessageCount = rows.getLong(9),
                    validElementCount = if (rows.isNull(10)) null else rows.getLong(10),
                    invalidElementCount = if (rows.isNull(11)) null else rows.getLong(11),
                    decoderVersion = rows.getString(12),
                    profileVersion = rows.getString(13),
                    status = FitFieldInventoryStatus.valueOf(rows.getString(14))
                ))
            }
        }

    @Synchronized fun updateFitUnmappedFieldStatus(
        field: StoredFitUnmappedField,
        status: FitFieldInventoryStatus
    ) {
        val changed = writableDatabase.update(
            "fit_unmapped_fields",
            ContentValues().apply { put("review_status", status.name) },
            """source_sha256 = ? AND global_message_number = ? AND field_kind = ?
               AND field_number = ? AND base_type_code = ? AND field_size = ?
               AND developer_data_index = ? AND classification = ?""",
            arrayOf(
                field.sourceSha256, field.globalMessageNumber.toString(), field.kind.name,
                field.fieldNumber.toString(), (field.baseTypeCode ?: -1).toString(),
                field.fieldSize.toString(), (field.developerDataIndex ?: -1).toString(),
                field.classification
            )
        )
        check(changed == 1) { "FIT field signature not found" }
    }

    fun fitDeviceMetadata(): List<FitHistoricalObservations.DeviceMetadata> =
        readableDatabase.rawQuery(
            """SELECT source_sha256, record_ordinal, observed_at_ms, device_type_code,
                      manufacturer_code, product_code, software_version, battery_voltage_volts,
                      battery_status_code, battery_level_percent
               FROM fit_device_metadata ORDER BY observed_at_ms, source_sha256, record_ordinal""",
            null
        ).use { rows ->
            buildList {
                while (rows.moveToNext()) add(FitHistoricalObservations.DeviceMetadata(
                    sourceFileSha256 = rows.getString(0),
                    recordOrdinal = rows.getInt(1),
                    timestampEpochMillis = if (rows.isNull(2)) null else rows.getLong(2),
                    deviceTypeCode = if (rows.isNull(3)) null else rows.getInt(3),
                    manufacturerCode = if (rows.isNull(4)) null else rows.getInt(4),
                    productCode = if (rows.isNull(5)) null else rows.getInt(5),
                    softwareVersion = if (rows.isNull(6)) null else rows.getDouble(6),
                    batteryVoltageVolts = if (rows.isNull(7)) null else rows.getDouble(7),
                    batteryStatusCode = if (rows.isNull(8)) null else rows.getInt(8),
                    batteryLevelPercent = if (rows.isNull(9)) null else rows.getInt(9)
                ))
            }
        }

    /** Persist metadata from verified Device XML only; filenames, paths and XML identifiers are excluded. */
    @Synchronized fun storeRawDeviceMetadata(record: RawDeviceMetadataRecord) {
        require(isValidRawDeviceMetadata(record)) { "Invalid whitelisted raw device metadata" }
        val db = writableDatabase
        db.beginTransaction()
        try {
            db.execSQL(
                """INSERT OR IGNORE INTO raw_device_metadata
                   (source_sha256, file_bytes, verified_at_ms, parser_version, software_version,
                    mass_storage_mode, transfer_direction_count)
                   VALUES (?, ?, ?, ?, ?, ?, ?)""",
                arrayOf<Any?>(
                    record.sourceSha256, record.fileBytes, record.verifiedAtEpochMillis, record.parserVersion,
                    record.softwareVersion, record.massStorageMode?.let { if (it) 1 else 0 },
                    record.transferDirectionElementCount
                )
            )
            db.rawQuery(
                """SELECT file_bytes, parser_version, software_version, mass_storage_mode,
                          transfer_direction_count
                   FROM raw_device_metadata WHERE source_sha256 = ?""",
                arrayOf(record.sourceSha256)
            ).use { row ->
                check(row.moveToFirst() && row.getLong(0) == record.fileBytes &&
                    row.getString(1) == record.parserVersion &&
                    nullableStringMatches(row, 2, record.softwareVersion) &&
                    nullableBooleanMatches(row, 3, record.massStorageMode) &&
                    row.getInt(4) == record.transferDirectionElementCount) {
                    "Raw metadata conflicts with previously indexed content hash"
                }
            }
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
    }

    fun rawDeviceMetadata(): List<RawDeviceMetadataRecord> =
        readableDatabase.rawQuery(
            """SELECT source_sha256, file_bytes, verified_at_ms, parser_version, software_version,
                      mass_storage_mode, transfer_direction_count
               FROM raw_device_metadata ORDER BY verified_at_ms, source_sha256""",
            null
        ).use { rows ->
            buildList {
                while (rows.moveToNext()) add(RawDeviceMetadataRecord(
                    sourceSha256 = rows.getString(0),
                    fileBytes = rows.getLong(1),
                    verifiedAtEpochMillis = rows.getLong(2),
                    parserVersion = rows.getString(3),
                    softwareVersion = if (rows.isNull(4)) null else rows.getString(4),
                    massStorageMode = if (rows.isNull(5)) null else rows.getInt(5) == 1,
                    transferDirectionElementCount = rows.getInt(6)
                ))
            }
        }

    /** Store a single locally generated JSONL event immediately, without a second BLE path. */
    @Synchronized fun insertLine(line: String): Boolean {
        return insertLine(writableDatabase, line)
    }

    /** Raw measured readings in chronological order, not inferred values. */
    fun readingLines(): List<String> {
        val result = ArrayList<String>()
        readableDatabase.rawQuery(
            "SELECT payload_json FROM events WHERE kind = ? ORDER BY received_at_utc, id",
            arrayOf("reading")
        ).use { cursor ->
            while (cursor.moveToNext()) result.add(cursor.getString(0))
        }
        return result
    }

    fun historicalObservationWindow(window: ExportWindow): List<RawExportRecord> =
        readableDatabase.rawQuery(
            """SELECT device_id, metric, measured_at_ms, value, unit, sample_id, received_at_ms
               FROM observations WHERE measured_at_ms >= ? AND measured_at_ms < ?
               ORDER BY measured_at_ms, device_id, metric, sample_id""",
            arrayOf(window.startInclusiveEpochMillis.toString(), window.endExclusiveEpochMillis.toString())
        ).use { rows ->
            buildList {
                while (rows.moveToNext()) add(RawExportRecord(
                    deviceId = rows.getString(0),
                    metric = rows.getString(1),
                    measuredAtEpochMillis = rows.getLong(2),
                    value = rows.getDouble(3),
                    unit = rows.getString(4),
                    sampleId = rows.getString(5).ifEmpty { null },
                    receivedAtEpochMillis = if (rows.isNull(6)) null else rows.getLong(6)
                ))
            }
        }

    /**
     * One-time, idempotent import of the former generic readings database.
     * The old file is retained as a safety backup, never used for new writes.
     */
    @Synchronized fun importLegacyReadings(): Int {
        val legacy = contextRef.getDatabasePath("pulselibre_readings.db")
        if (!legacy.exists()) return 0
        var imported = 0
        val old = LocalReadingStore(contextRef)
        try {
            old.readWindow(ExportWindow(0, Long.MAX_VALUE)).forEach { record ->
                val line = JSONObject()
                    .put("schema", 1)
                    .put("type", "legacy_record")
                    .put("received_at_utc", java.time.Instant.ofEpochMilli(
                        record.receivedAtEpochMillis ?: record.measuredAtEpochMillis).toString())
                    .put("device_id", record.deviceId)
                    .put("metric", record.metric)
                    .put("measured_at_ms", record.measuredAtEpochMillis)
                    .put("value", record.value)
                    .put("unit", record.unit)
                    .put("sample_id", record.sampleId ?: "")
                    .put("received_at_ms", record.receivedAtEpochMillis ?: JSONObject.NULL)
                    .toString()
                if (insertLine(line)) imported++
            }
        } finally { old.close() }
        return imported
    }

    fun legacyLines(): List<String> {
        val result = ArrayList<String>()
        readableDatabase.rawQuery(
            "SELECT payload_json FROM events WHERE kind = ? ORDER BY received_at_utc, id",
            arrayOf("legacy_record")
        ).use { cursor ->
            while (cursor.moveToNext()) result.add(cursor.getString(0))
        }
        return result
    }

    fun allLines(): List<String> {
        val result = ArrayList<String>()
        readableDatabase.rawQuery(
            "SELECT payload_json FROM events ORDER BY received_at_utc, id", null
        ).use { cursor ->
            while (cursor.moveToNext()) result.add(cursor.getString(0))
        }
        return result
    }

    fun eventBytes(): Long = readableDatabase.rawQuery(
        "SELECT COALESCE(SUM(LENGTH(CAST(payload_json AS BLOB)) + 1), 0) FROM events", null
    ).use { cursor ->
        cursor.moveToFirst()
        cursor.getLong(0)
    }

    fun eventCount(): Long = readableDatabase.rawQuery("SELECT COUNT(*) FROM events", null).use {
        it.moveToFirst()
        it.getLong(0)
    }

    /** Run a callback against a consistent read snapshot of both local SQLite stores. */
    @Synchronized internal fun <T> readStoredSnapshot(block: (SQLiteDatabase) -> T): T =
        withLegacyDatabase { db ->
            db.beginTransaction()
            try {
                val result = block(db)
                db.setTransactionSuccessful()
                result
            } finally {
                db.endTransaction()
            }
        }

    /** Replace both local stores in a single attached-database transaction. */
    @Synchronized internal fun replaceAllStoredRows(
        events: Sequence<String>,
        readings: Sequence<RawExportRecord>,
        cursors: Sequence<LocalSyncCursor>,
        fitSources: Sequence<StoredFitSource> = emptySequence(),
        fitProvenance: Sequence<StoredFitProvenance> = emptySequence(),
        canonicalObservations: Sequence<RawExportRecord> = emptySequence(),
        canonicalCursors: Sequence<LocalSyncCursor> = emptySequence(),
        canonicalFitSources: Sequence<StoredFitSource> = emptySequence(),
        canonicalFitProvenance: Sequence<StoredFitProvenance> = emptySequence(),
        canonicalDeviceMetadata: Sequence<FitHistoricalObservations.DeviceMetadata> = emptySequence(),
        canonicalRawDeviceMetadata: Sequence<RawDeviceMetadataRecord> = emptySequence(),
        canonicalUnmappedFitFields: Sequence<StoredFitUnmappedField> = emptySequence(),
        canonicalFitGenerations: Sequence<StoredFitDecoderGeneration> = emptySequence(),
        canonicalFitGenerationObservations: Sequence<StoredFitGenerationObservation> = emptySequence(),
        canonicalActiveFitGenerations: Sequence<StoredActiveFitGeneration> = emptySequence(),
        canonicalFitReviewCandidates: Sequence<StoredFitReviewCandidate> = emptySequence(),
        generationSnapshotProvided: Boolean = false,
        markHistoricalCopyComplete: Boolean = false
    ) = withLegacyDatabase { db ->
        db.beginTransaction()
        try {
            db.execSQL("DELETE FROM main.events")
            db.execSQL("DELETE FROM main.fit_active_generations")
            db.execSQL("DELETE FROM main.fit_review_candidates")
            db.execSQL("DELETE FROM main.fit_generation_observations")
            db.execSQL("DELETE FROM main.fit_decoder_generations")
            db.execSQL("DELETE FROM main.fit_observation_provenance")
            db.execSQL("DELETE FROM main.fit_unmapped_fields")
            db.execSQL("DELETE FROM main.fit_device_metadata")
            db.execSQL("DELETE FROM main.raw_device_metadata")
            db.execSQL("DELETE FROM main.observations")
            db.execSQL("DELETE FROM main.fit_sources")
            db.execSQL("DELETE FROM main.sync_cursors")
            db.execSQL("DELETE FROM main.storage_migrations")
            db.execSQL("DELETE FROM legacy.fit_observation_provenance")
            db.execSQL("DELETE FROM legacy.readings")
            db.execSQL("DELETE FROM legacy.fit_sources")
            db.execSQL("DELETE FROM legacy.sync_cursors")

            val eventInsert = db.compileStatement(
                "INSERT INTO main.events(line_sha256, received_at_utc, kind, service, payload_json, dashboard_time) VALUES(?, ?, ?, ?, ?, ?)"
            )
            events.forEach { line ->
                require(isValidStoredLine(line)) { "Ungültiger Ereigniseintrag im Backup" }
                val item = JSONObject(line)
                eventInsert.clearBindings()
                eventInsert.bindString(1, lineSha256(line))
                eventInsert.bindString(2, item.getString("received_at_utc"))
                eventInsert.bindString(3, item.getString("type"))
                if (item.has("service") && !item.isNull("service")) eventInsert.bindLong(4, item.getLong("service"))
                else eventInsert.bindNull(4)
                eventInsert.bindString(5, line)
                val key = if (item.getString("type") == "reading") timeKey(item.getString("received_at_utc")) else null
                if (key == null) eventInsert.bindNull(6) else eventInsert.bindString(6, key)
                eventInsert.executeInsert()
            }

            val readingInsert = db.compileStatement(
                """INSERT INTO legacy.readings
                   (device_id, metric, measured_at_ms, value, unit, sample_id, received_at_ms)
                   VALUES(?, ?, ?, ?, ?, ?, ?)"""
            )
            readings.forEach { record ->
                require(isValidStoredReading(record)) { "Ungültiger Messwert im Backup" }
                readingInsert.clearBindings()
                readingInsert.bindString(1, record.deviceId)
                readingInsert.bindString(2, record.metric)
                readingInsert.bindLong(3, record.measuredAtEpochMillis)
                readingInsert.bindDouble(4, record.value)
                readingInsert.bindString(5, record.unit)
                readingInsert.bindString(6, record.sampleId ?: "")
                if (record.receivedAtEpochMillis == null) readingInsert.bindNull(7)
                else readingInsert.bindLong(7, record.receivedAtEpochMillis)
                readingInsert.executeInsert()
            }

            val sourceInsert = db.compileStatement(
                """INSERT INTO legacy.fit_sources
                   (source_sha256, file_bytes, relative_path, validated_at_ms, decoder_version, profile_version)
                   VALUES(?, ?, ?, ?, ?, ?)"""
            )
            fitSources.forEach { source ->
                require(source.sha256.matches(Regex("[0-9a-f]{64}")) && source.fileBytes > 0 &&
                    source.validatedAtEpochMillis >= 0 &&
                    source.relativePath == "verified_fit/${source.sha256}.fit" &&
                    source.decoderVersion.isNotBlank() && source.profileVersion.isNotBlank()) {
                    "Ungültige FIT-Quellmetadaten im Backup"
                }
                sourceInsert.clearBindings()
                sourceInsert.bindString(1, source.sha256)
                sourceInsert.bindLong(2, source.fileBytes)
                sourceInsert.bindString(3, source.relativePath)
                sourceInsert.bindLong(4, source.validatedAtEpochMillis)
                sourceInsert.bindString(5, source.decoderVersion)
                sourceInsert.bindString(6, source.profileVersion)
                sourceInsert.executeInsert()
            }

            val provenanceInsert = db.compileStatement(
                """INSERT INTO legacy.fit_observation_provenance
                   (sample_id, device_id, metric, measured_at_ms, source_sha256, record_ordinal,
                    global_message_number, field_number, quality, decoder_version)
                   VALUES(?, ?, ?, ?, ?, ?, ?, ?, ?, ?)"""
            )
            fitProvenance.forEach { item ->
                require(item.sampleId.isNotBlank() && item.deviceId.isNotBlank() && item.metric.isNotBlank() &&
                    item.measuredAtEpochMillis >= 0 && item.sourceSha256.matches(Regex("[0-9a-f]{64}")) &&
                    item.recordOrdinal >= 0 && item.globalMessageNumber >= 0 && item.fieldNumber >= 0 &&
                    item.quality == "PROFILE_VALID" && item.decoderVersion.isNotBlank()) {
                    "Ungültige FIT-Feldherkunft im Backup"
                }
                provenanceInsert.clearBindings()
                provenanceInsert.bindString(1, item.sampleId)
                provenanceInsert.bindString(2, item.deviceId)
                provenanceInsert.bindString(3, item.metric)
                provenanceInsert.bindLong(4, item.measuredAtEpochMillis)
                provenanceInsert.bindString(5, item.sourceSha256)
                provenanceInsert.bindLong(6, item.recordOrdinal.toLong())
                provenanceInsert.bindLong(7, item.globalMessageNumber.toLong())
                provenanceInsert.bindLong(8, item.fieldNumber.toLong())
                provenanceInsert.bindString(9, item.quality)
                provenanceInsert.bindString(10, item.decoderVersion)
                provenanceInsert.executeInsert()
            }

            val cursorInsert = db.compileStatement(
                "INSERT INTO legacy.sync_cursors(device_id, last_committed_ms) VALUES(?, ?)"
            )
            cursors.forEach { value ->
                require(value.deviceId.isNotBlank() && value.lastCommittedEpochMillis >= 0) {
                    "Ungültiger Synchronisationsstand im Backup"
                }
                cursorInsert.clearBindings()
                cursorInsert.bindString(1, value.deviceId)
                cursorInsert.bindLong(2, value.lastCommittedEpochMillis)
                cursorInsert.executeInsert()
            }

            val canonicalReadingInsert = db.compileStatement(
                """INSERT INTO main.observations
                   (device_id, metric, measured_at_ms, value, unit, sample_id, received_at_ms)
                   VALUES(?, ?, ?, ?, ?, ?, ?)"""
            )
            canonicalObservations.forEach { record ->
                require(isValidStoredReading(record)) { "Ungültiger kanonischer Messwert im Backup" }
                canonicalReadingInsert.clearBindings()
                canonicalReadingInsert.bindString(1, record.deviceId)
                canonicalReadingInsert.bindString(2, record.metric)
                canonicalReadingInsert.bindLong(3, record.measuredAtEpochMillis)
                canonicalReadingInsert.bindDouble(4, record.value)
                canonicalReadingInsert.bindString(5, record.unit)
                canonicalReadingInsert.bindString(6, record.sampleId ?: "")
                if (record.receivedAtEpochMillis == null) canonicalReadingInsert.bindNull(7)
                else canonicalReadingInsert.bindLong(7, record.receivedAtEpochMillis)
                canonicalReadingInsert.executeInsert()
            }

            val canonicalCursorInsert = db.compileStatement(
                "INSERT INTO main.sync_cursors(device_id, last_committed_ms) VALUES(?, ?)"
            )
            canonicalCursors.forEach { value ->
                require(value.deviceId.isNotBlank() && value.lastCommittedEpochMillis >= 0) {
                    "Ungültiger kanonischer Synchronisationsstand im Backup"
                }
                canonicalCursorInsert.clearBindings()
                canonicalCursorInsert.bindString(1, value.deviceId)
                canonicalCursorInsert.bindLong(2, value.lastCommittedEpochMillis)
                canonicalCursorInsert.executeInsert()
            }

            val canonicalSourceInsert = db.compileStatement(
                """INSERT INTO main.fit_sources
                   (source_sha256, file_bytes, relative_path, validated_at_ms, decoder_version, profile_version)
                   VALUES(?, ?, ?, ?, ?, ?)"""
            )
            canonicalFitSources.forEach { source ->
                require(source.sha256.matches(Regex("[0-9a-f]{64}")) && source.fileBytes > 0 &&
                    source.validatedAtEpochMillis >= 0 &&
                    source.relativePath == "verified_fit/${source.sha256}.fit" &&
                    source.decoderVersion.isNotBlank() && source.profileVersion.isNotBlank()) {
                    "Ungültige kanonische FIT-Quellmetadaten im Backup"
                }
                canonicalSourceInsert.clearBindings()
                canonicalSourceInsert.bindString(1, source.sha256)
                canonicalSourceInsert.bindLong(2, source.fileBytes)
                canonicalSourceInsert.bindString(3, source.relativePath)
                canonicalSourceInsert.bindLong(4, source.validatedAtEpochMillis)
                canonicalSourceInsert.bindString(5, source.decoderVersion)
                canonicalSourceInsert.bindString(6, source.profileVersion)
                canonicalSourceInsert.executeInsert()
            }

            val deviceMetadataInsert = db.compileStatement(
                """INSERT INTO main.fit_device_metadata
                   (source_sha256, record_ordinal, observed_at_ms, device_type_code,
                    manufacturer_code, product_code, software_version, battery_voltage_volts,
                    battery_status_code, battery_level_percent)
                   VALUES(?, ?, ?, ?, ?, ?, ?, ?, ?, ?)"""
            )
            canonicalDeviceMetadata.forEach { item ->
                require(isValidDeviceMetadata(item)) { "Ungültige FIT-Gerätemetadaten im Backup" }
                deviceMetadataInsert.clearBindings()
                deviceMetadataInsert.bindString(1, item.sourceFileSha256)
                deviceMetadataInsert.bindLong(2, item.recordOrdinal.toLong())
                if (item.timestampEpochMillis == null) deviceMetadataInsert.bindNull(3)
                else deviceMetadataInsert.bindLong(3, item.timestampEpochMillis)
                if (item.deviceTypeCode == null) deviceMetadataInsert.bindNull(4)
                else deviceMetadataInsert.bindLong(4, item.deviceTypeCode.toLong())
                if (item.manufacturerCode == null) deviceMetadataInsert.bindNull(5)
                else deviceMetadataInsert.bindLong(5, item.manufacturerCode.toLong())
                if (item.productCode == null) deviceMetadataInsert.bindNull(6)
                else deviceMetadataInsert.bindLong(6, item.productCode.toLong())
                if (item.softwareVersion == null) deviceMetadataInsert.bindNull(7)
                else deviceMetadataInsert.bindDouble(7, item.softwareVersion)
                if (item.batteryVoltageVolts == null) deviceMetadataInsert.bindNull(8)
                else deviceMetadataInsert.bindDouble(8, item.batteryVoltageVolts)
                if (item.batteryStatusCode == null) deviceMetadataInsert.bindNull(9)
                else deviceMetadataInsert.bindLong(9, item.batteryStatusCode.toLong())
                if (item.batteryLevelPercent == null) deviceMetadataInsert.bindNull(10)
                else deviceMetadataInsert.bindLong(10, item.batteryLevelPercent.toLong())
                deviceMetadataInsert.executeInsert()
            }

            val rawDeviceMetadataInsert = db.compileStatement(
                """INSERT INTO main.raw_device_metadata
                   (source_sha256, file_bytes, verified_at_ms, parser_version, software_version,
                    mass_storage_mode, transfer_direction_count)
                   VALUES(?, ?, ?, ?, ?, ?, ?)"""
            )
            canonicalRawDeviceMetadata.forEach { item ->
                require(isValidRawDeviceMetadata(item)) { "Ungültige Raw-Gerätemetadaten im Backup" }
                rawDeviceMetadataInsert.clearBindings()
                rawDeviceMetadataInsert.bindString(1, item.sourceSha256)
                rawDeviceMetadataInsert.bindLong(2, item.fileBytes)
                rawDeviceMetadataInsert.bindLong(3, item.verifiedAtEpochMillis)
                rawDeviceMetadataInsert.bindString(4, item.parserVersion)
                if (item.softwareVersion == null) rawDeviceMetadataInsert.bindNull(5)
                else rawDeviceMetadataInsert.bindString(5, item.softwareVersion)
                if (item.massStorageMode == null) rawDeviceMetadataInsert.bindNull(6)
                else rawDeviceMetadataInsert.bindLong(6, if (item.massStorageMode) 1 else 0)
                rawDeviceMetadataInsert.bindLong(7, item.transferDirectionElementCount.toLong())
                rawDeviceMetadataInsert.executeInsert()
            }
            val unmappedFitFieldInsert = db.compileStatement(
                """INSERT INTO main.fit_unmapped_fields
                   (source_sha256, global_message_number, field_kind, field_number, base_type_code,
                    field_size, developer_data_index, classification, definition_count,
                    observed_message_count, valid_element_count, invalid_element_count,
                    decoder_version, profile_version, review_status)
                   VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)"""
            )
            canonicalUnmappedFitFields.forEach { item ->
                require(item.sourceSha256.matches(Regex("[0-9a-f]{64}")) &&
                    item.globalMessageNumber in 0..65535 && item.fieldNumber in 0..255 &&
                    item.fieldSize in 1..255 && item.definitionCount in 0..1_000_000 &&
                    item.observedMessageCount in 0..1_000_000) {
                    "Ungültige FIT-Feldinventur im Backup"
                }
                unmappedFitFieldInsert.clearBindings()
                unmappedFitFieldInsert.bindString(1, item.sourceSha256)
                unmappedFitFieldInsert.bindLong(2, item.globalMessageNumber.toLong())
                unmappedFitFieldInsert.bindString(3, item.kind.name)
                unmappedFitFieldInsert.bindLong(4, item.fieldNumber.toLong())
                unmappedFitFieldInsert.bindLong(5, (item.baseTypeCode ?: -1).toLong())
                unmappedFitFieldInsert.bindLong(6, item.fieldSize.toLong())
                unmappedFitFieldInsert.bindLong(7, (item.developerDataIndex ?: -1).toLong())
                unmappedFitFieldInsert.bindString(8, item.classification)
                unmappedFitFieldInsert.bindLong(9, item.definitionCount)
                unmappedFitFieldInsert.bindLong(10, item.observedMessageCount)
                if (item.validElementCount == null) unmappedFitFieldInsert.bindNull(11)
                else unmappedFitFieldInsert.bindLong(11, item.validElementCount)
                if (item.invalidElementCount == null) unmappedFitFieldInsert.bindNull(12)
                else unmappedFitFieldInsert.bindLong(12, item.invalidElementCount)
                unmappedFitFieldInsert.bindString(13, item.decoderVersion)
                unmappedFitFieldInsert.bindString(14, item.profileVersion)
                unmappedFitFieldInsert.bindString(15, item.status.name)
                unmappedFitFieldInsert.executeInsert()
            }

            val canonicalProvenanceInsert = db.compileStatement(
                """INSERT INTO main.fit_observation_provenance
                   (sample_id, device_id, metric, measured_at_ms, source_sha256, record_ordinal,
                    global_message_number, field_number, quality, decoder_version)
                   VALUES(?, ?, ?, ?, ?, ?, ?, ?, ?, ?)"""
            )
            canonicalFitProvenance.forEach { item ->
                require(item.sampleId.isNotBlank() && item.deviceId.isNotBlank() && item.metric.isNotBlank() &&
                    item.measuredAtEpochMillis >= 0 && item.sourceSha256.matches(Regex("[0-9a-f]{64}")) &&
                    item.recordOrdinal >= 0 && item.globalMessageNumber >= 0 && item.fieldNumber >= 0 &&
                    item.quality == "PROFILE_VALID" && item.decoderVersion.isNotBlank()) {
                    "Ungültige kanonische FIT-Feldherkunft im Backup"
                }
                canonicalProvenanceInsert.clearBindings()
                canonicalProvenanceInsert.bindString(1, item.sampleId)
                canonicalProvenanceInsert.bindString(2, item.deviceId)
                canonicalProvenanceInsert.bindString(3, item.metric)
                canonicalProvenanceInsert.bindLong(4, item.measuredAtEpochMillis)
                canonicalProvenanceInsert.bindString(5, item.sourceSha256)
                canonicalProvenanceInsert.bindLong(6, item.recordOrdinal.toLong())
                canonicalProvenanceInsert.bindLong(7, item.globalMessageNumber.toLong())
                canonicalProvenanceInsert.bindLong(8, item.fieldNumber.toLong())
                canonicalProvenanceInsert.bindString(9, item.quality)
                canonicalProvenanceInsert.bindString(10, item.decoderVersion)
                canonicalProvenanceInsert.executeInsert()
            }
            val generations = canonicalFitGenerations.toList()
            val generationObservations = canonicalFitGenerationObservations.toList()
            val activeGenerations = canonicalActiveFitGenerations.toList()
            val reviewCandidates = canonicalFitReviewCandidates.toList()
            if (generationSnapshotProvided) {
                require(generations.map { it.generationId }.distinct().size == generations.size)
                val observationsByGeneration = generationObservations.groupBy { it.generationId }
                require(
                    observationsByGeneration.keys.all { id -> generations.any { it.generationId == id } } &&
                        generations.all { generation ->
                            val rows = observationsByGeneration[generation.generationId].orEmpty()
                            rows.size.toLong() == generation.outputCount &&
                                FitDecoderGeneration.outputDigest(rows) == generation.outputDigest
                        }
                ) { "FIT-Decodergenerationen im Backup sind unvollständig oder inkonsistent" }
                require(generations.all { generation ->
                    generation.sourceSha256.matches(Regex("[0-9a-f]{64}")) &&
                        generation.generationId == FitDecoderGeneration.id(
                            generation.sourceSha256, generation.decoderVersion, generation.profileVersion
                        ) && generation.outputDigest.matches(Regex("[0-9a-f]{64}")) &&
                        generation.outputCount >= 0 && generation.completedAtEpochMillis >= 0
                }) { "Ungültige FIT-Decodergeneration im Backup" }
                require(activeGenerations.map { it.sourceSha256 }.distinct().size == activeGenerations.size &&
                    activeGenerations.all { active ->
                        generations.any {
                            it.generationId == active.generationId &&
                                it.sourceSha256 == active.sourceSha256
                        }
                    }) { "Aktive FIT-Generation fehlt oder ist mehrdeutig" }

                val generationInsert = db.compileStatement(
                    """INSERT INTO main.fit_decoder_generations
                       (generation_id, source_sha256, decoder_version, profile_version,
                        output_digest, output_count, completed_at_ms) VALUES(?, ?, ?, ?, ?, ?, ?)"""
                )
                generations.forEach { item ->
                    generationInsert.clearBindings()
                    generationInsert.bindString(1, item.generationId)
                    generationInsert.bindString(2, item.sourceSha256)
                    generationInsert.bindString(3, item.decoderVersion)
                    generationInsert.bindString(4, item.profileVersion)
                    generationInsert.bindString(5, item.outputDigest)
                    generationInsert.bindLong(6, item.outputCount)
                    generationInsert.bindLong(7, item.completedAtEpochMillis)
                    generationInsert.executeInsert()
                }
                val generationObservationInsert = db.compileStatement(
                    """INSERT INTO main.fit_generation_observations
                       (generation_id, sample_id, device_id, metric, measured_at_ms, value, unit,
                        received_at_ms, record_ordinal, global_message_number, field_number, quality,
                        timestamp_origin) VALUES(?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)"""
                )
                generationObservations.forEach { item ->
                    require(item.sampleId.isNotBlank() && item.deviceId.isNotBlank() &&
                        item.metric.isNotBlank() && item.measuredAtEpochMillis >= 0 &&
                        item.value.isFinite() && item.unit.isNotBlank() &&
                        (item.receivedAtEpochMillis == null || item.receivedAtEpochMillis >= 0) &&
                        item.recordOrdinal >= 0 && item.globalMessageNumber >= 0 && item.fieldNumber >= 0 &&
                        item.quality == "PROFILE_VALID" &&
                        item.timestampOrigin in setOf(
                            "DIRECT", "RECONSTRUCTED_TS16", "RECONSTRUCTED_COMPRESSED", "UNKNOWN"
                        )) {
                        "Ungültige FIT-Beobachtung in einer Decodergeneration"
                    }
                    generationObservationInsert.clearBindings()
                    generationObservationInsert.bindString(1, item.generationId)
                    generationObservationInsert.bindString(2, item.sampleId)
                    generationObservationInsert.bindString(3, item.deviceId)
                    generationObservationInsert.bindString(4, item.metric)
                    generationObservationInsert.bindLong(5, item.measuredAtEpochMillis)
                    generationObservationInsert.bindDouble(6, item.value)
                    generationObservationInsert.bindString(7, item.unit)
                    if (item.receivedAtEpochMillis == null) generationObservationInsert.bindNull(8)
                    else generationObservationInsert.bindLong(8, item.receivedAtEpochMillis)
                    generationObservationInsert.bindLong(9, item.recordOrdinal.toLong())
                    generationObservationInsert.bindLong(10, item.globalMessageNumber.toLong())
                    generationObservationInsert.bindLong(11, item.fieldNumber.toLong())
                    generationObservationInsert.bindString(12, item.quality)
                    generationObservationInsert.bindString(13, item.timestampOrigin)
                    generationObservationInsert.executeInsert()
                }
                val activeGenerationInsert = db.compileStatement(
                    "INSERT INTO main.fit_active_generations(source_sha256, generation_id) VALUES(?, ?)"
                )
                activeGenerations.forEach { item ->
                    activeGenerationInsert.clearBindings()
                    activeGenerationInsert.bindString(1, item.sourceSha256)
                    activeGenerationInsert.bindString(2, item.generationId)
                    activeGenerationInsert.executeInsert()
                }
                db.rawQuery(
                    """SELECT COUNT(*) FROM main.fit_sources s
                       LEFT JOIN main.fit_active_generations a ON a.source_sha256 = s.source_sha256
                       WHERE a.source_sha256 IS NULL""", null
                ).use { missing ->
                    check(missing.moveToFirst() && missing.getLong(0) == 0L) {
                        "Mindestens eine FIT-Quelle hat keine aktive Decodergeneration"
                    }
                }
            } else {
                migrateExistingFitObservationsToGeneration(db)
            }
            val reviewInsert = db.compileStatement(
                """INSERT INTO main.fit_review_candidates
                   (sample_id, device_id, metric, measured_at_ms, value, unit, source_sha256,
                    record_ordinal, global_message_number, field_number, quality, timestamp_origin,
                    decoder_version) VALUES(?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)"""
            )
            reviewCandidates.forEach { item ->
                require(item.sampleId.isNotBlank() && item.deviceId.isNotBlank() &&
                    item.metric in setOf("spo2", "stress", "respiration") &&
                    (item.measuredAtEpochMillis == null || item.measuredAtEpochMillis >= 0) &&
                    item.value.isFinite() && item.unit.isNotBlank() &&
                    item.sourceSha256.matches(Regex("[0-9a-f]{64}")) && item.recordOrdinal > 0 &&
                    item.globalMessageNumber >= 0 && item.fieldNumber >= 0 &&
                    item.quality == "UNTRUSTED_PROFILE_CANDIDATE" &&
                    item.timestampOrigin in setOf(
                        "DIRECT", "RECONSTRUCTED_TS16", "RECONSTRUCTED_COMPRESSED", "UNKNOWN"
                    ) && item.decoderVersion.isNotBlank()) {
                    "Ungültiger untrusted FIT-Kandidat im Backup"
                }
                reviewInsert.clearBindings()
                reviewInsert.bindString(1, item.sampleId)
                reviewInsert.bindString(2, item.deviceId)
                reviewInsert.bindString(3, item.metric)
                if (item.measuredAtEpochMillis == null) reviewInsert.bindNull(4)
                else reviewInsert.bindLong(4, item.measuredAtEpochMillis)
                reviewInsert.bindDouble(5, item.value)
                reviewInsert.bindString(6, item.unit)
                reviewInsert.bindString(7, item.sourceSha256)
                reviewInsert.bindLong(8, item.recordOrdinal.toLong())
                reviewInsert.bindLong(9, item.globalMessageNumber.toLong())
                reviewInsert.bindLong(10, item.fieldNumber.toLong())
                reviewInsert.bindString(11, item.quality)
                reviewInsert.bindString(12, item.timestampOrigin)
                reviewInsert.bindString(13, item.decoderVersion)
                reviewInsert.executeInsert()
            }
            if (markHistoricalCopyComplete) {
                val historicalCounts = listOf(
                    "readings", "sync_cursors", "fit_sources", "fit_observation_provenance"
                ).map { table ->
                    db.rawQuery("SELECT COUNT(*) FROM legacy.$table", null).use {
                        it.moveToFirst()
                        it.getLong(0)
                    }
                }
                db.execSQL(
                    """INSERT OR REPLACE INTO main.storage_migrations
                       (migration_key, source_readings, source_cursors, source_fit_sources,
                        source_fit_provenance, completed_at_ms)
                       VALUES('historical-v1-v2-copy', ?, ?, ?, ?, ?)""",
                    arrayOf(
                        historicalCounts[0], historicalCounts[1], historicalCounts[2],
                        historicalCounts[3], System.currentTimeMillis()
                    )
                )
            }
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
    }

    private fun insertLine(db: SQLiteDatabase, line: String): Boolean {
        val item = try { JSONObject(line) } catch (_: Exception) { return false }
        val kind = item.optString("type")
        if (item.optInt("schema") != 1 || kind.isBlank()) return false
        val values = ContentValues().apply {
            put("line_sha256", lineSha256(line))
            put("received_at_utc", item.optString("received_at_utc"))
            if (kind == "reading") put("dashboard_time", timeKey(item.optString("received_at_utc")))
            put("kind", kind)
            if (item.has("service") && !item.isNull("service")) put("service", item.optInt("service"))
            put("payload_json", line)
        }
        return db.insertWithOnConflict("events", null, values, SQLiteDatabase.CONFLICT_IGNORE) != -1L
    }

    private inline fun <T> withLegacyDatabase(block: (SQLiteDatabase) -> T): T {
        LocalReadingStore(contextRef).use { it.writableDatabase }
        val db = writableDatabase
        db.execSQL("ATTACH DATABASE ? AS legacy", arrayOf(contextRef.getDatabasePath("pulselibre_readings.db").absolutePath))
        return try { block(db) }
        finally { runCatching { db.execSQL("DETACH DATABASE legacy") } }
    }

    companion object {
        private val keyFormat = java.time.format.DateTimeFormatterBuilder().appendInstant(9).toFormatter()
        internal fun timeKey(time: java.time.Instant): String = keyFormat.format(time)
        private fun timeKey(timestamp: String): String? = try { timeKey(java.time.Instant.parse(timestamp)) }
            catch (_: java.time.format.DateTimeParseException) { null }

        fun isValidStoredLine(line: String): Boolean = try {
            val item = JSONObject(line)
            item.optInt("schema") == 1 && item.optString("type").isNotBlank() &&
                item.optString("received_at_utc").isNotBlank()
        } catch (_: Exception) { false }

        fun isValidStoredReading(record: RawExportRecord): Boolean = record.deviceId.isNotBlank() &&
            record.metric.isNotBlank() && record.unit.isNotBlank() && record.measuredAtEpochMillis >= 0 &&
            record.value.isFinite() && (record.receivedAtEpochMillis == null || record.receivedAtEpochMillis >= 0)

        internal fun isValidDeviceMetadata(item: FitHistoricalObservations.DeviceMetadata): Boolean =
            item.sourceFileSha256.matches(Regex("[0-9a-f]{64}")) && item.recordOrdinal > 0 &&
                (item.timestampEpochMillis == null || item.timestampEpochMillis >= 0) &&
                (item.deviceTypeCode == null || item.deviceTypeCode in 0..254) &&
                (item.manufacturerCode == null || item.manufacturerCode in 0..65534) &&
                (item.productCode == null || item.productCode in 0..65534) &&
                (item.softwareVersion == null || item.softwareVersion.isFinite() && item.softwareVersion in 0.0..655.34) &&
                (item.batteryVoltageVolts == null || item.batteryVoltageVolts.isFinite() && item.batteryVoltageVolts in 0.0..20.0) &&
                (item.batteryStatusCode == null || item.batteryStatusCode in 0..254) &&
                (item.batteryLevelPercent == null || item.batteryLevelPercent in 0..100)

        internal fun isValidRawDeviceMetadata(item: RawDeviceMetadataRecord): Boolean =
            item.sourceSha256.matches(Regex("[0-9a-f]{64}")) && item.fileBytes > 0 &&
                item.verifiedAtEpochMillis >= 0 &&
                item.parserVersion.matches(Regex("[A-Za-z0-9._+-]{1,32}")) &&
                (item.softwareVersion == null ||
                    item.softwareVersion.matches(Regex("[A-Za-z0-9._+-]{1,128}"))) &&
                item.transferDirectionElementCount in 0..10_000

        private fun nullableStringMatches(cursor: android.database.Cursor, column: Int, value: String?): Boolean =
            if (value == null) cursor.isNull(column)
            else !cursor.isNull(column) && cursor.getString(column) == value

        private fun nullableBooleanMatches(cursor: android.database.Cursor, column: Int, value: Boolean?): Boolean =
            if (value == null) cursor.isNull(column)
            else !cursor.isNull(column) && (cursor.getInt(column) == 1) == value

        private fun lineSha256(line: String): String = MessageDigest.getInstance("SHA-256")
            .digest(line.toByteArray(Charsets.UTF_8))
            .joinToString("") { "%02x".format(it.toInt() and 255) }
    }
}

data class LocalSyncCursor(val deviceId: String, val lastCommittedEpochMillis: Long)
