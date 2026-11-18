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
class CirqaDatabase(private val contextRef: Context) : SQLiteOpenHelper(contextRef, "cirqa_measurements.db", null, 3) {

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
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        require(oldVersion in 1..2 && newVersion == 3)
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
                    db.execSQL("UPDATE events SET dashboard_time = ? WHERE id = ?", arrayOf(timeKey(timestamp), id))
                }
                lastId = batch.last().first
            }
            createDashboardIndex(db)
        }
        createHistoricalSchema(db)
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
            if (priorCopy == sourceCounts) return

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
    @Synchronized fun replaceAllStoredRows(
        events: Sequence<String>,
        readings: Sequence<RawExportRecord>,
        cursors: Sequence<LocalSyncCursor>,
        fitSources: Sequence<StoredFitSource> = emptySequence(),
        fitProvenance: Sequence<StoredFitProvenance> = emptySequence()
    ) = withLegacyDatabase { db ->
        db.beginTransaction()
        try {
            db.execSQL("DELETE FROM main.events")
            db.execSQL("DELETE FROM main.fit_observation_provenance")
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

        private fun lineSha256(line: String): String = MessageDigest.getInstance("SHA-256")
            .digest(line.toByteArray(Charsets.UTF_8))
            .joinToString("") { "%02x".format(it.toInt() and 255) }
    }
}

data class LocalSyncCursor(val deviceId: String, val lastCommittedEpochMillis: Long)
