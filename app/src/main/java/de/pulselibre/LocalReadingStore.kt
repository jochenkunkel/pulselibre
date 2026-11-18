package de.pulselibre

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

data class StoredFitSource(
    val sha256: String,
    val fileBytes: Long,
    val relativePath: String,
    val validatedAtEpochMillis: Long,
    val decoderVersion: String,
    val profileVersion: String
)

data class StoredFitProvenance(
    val sampleId: String,
    val deviceId: String,
    val metric: String,
    val measuredAtEpochMillis: Long,
    val sourceSha256: String,
    val recordOrdinal: Int,
    val globalMessageNumber: Int,
    val fieldNumber: Int,
    val quality: String,
    val decoderVersion: String
)

/**
 * Private, offline SQLite store. The database contains only validated readings.
 * Each batch and its cursor are committed in one transaction.
 * No Bluetooth pairing, network access or Garmin-specific decoding occurs here.
 */
class LocalReadingStore(context: Context) : SQLiteOpenHelper(
    context.applicationContext, "pulselibre_readings.db", null, 2
) {
    override fun onConfigure(db: SQLiteDatabase) {
        super.onConfigure(db)
        db.setForeignKeyConstraintsEnabled(true)
    }

    override fun onCreate(db: SQLiteDatabase) {
        createReadingsSchema(db)
        createFitProvenanceSchema(db)
    }

    private fun createReadingsSchema(db: SQLiteDatabase) {
        db.execSQL(
            """CREATE TABLE readings (
                device_id TEXT NOT NULL,
                metric TEXT NOT NULL,
                measured_at_ms INTEGER NOT NULL,
                value REAL NOT NULL,
                unit TEXT NOT NULL,
                sample_id TEXT NOT NULL DEFAULT '',
                received_at_ms INTEGER,
                PRIMARY KEY(device_id, metric, measured_at_ms, sample_id)
            )"""
        )
        db.execSQL(
            """CREATE TABLE sync_cursors (
                device_id TEXT PRIMARY KEY NOT NULL,
                last_committed_ms INTEGER NOT NULL
            )"""
        )
        db.execSQL("CREATE INDEX readings_by_time ON readings(measured_at_ms)")
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        require(oldVersion == 1 && newVersion == 2) {
            "Missing database migration from $oldVersion to $newVersion"
        }
        // Additive only: existing readings and cursors remain byte-for-byte intact.
        createFitProvenanceSchema(db)
    }

    private fun createFitProvenanceSchema(db: SQLiteDatabase) {
        db.execSQL(
            """CREATE TABLE fit_sources (
                source_sha256 TEXT PRIMARY KEY NOT NULL
                    CHECK(length(source_sha256) = 64),
                file_bytes INTEGER NOT NULL CHECK(file_bytes > 0),
                relative_path TEXT NOT NULL,
                validated_at_ms INTEGER NOT NULL CHECK(validated_at_ms >= 0),
                decoder_version TEXT NOT NULL,
                profile_version TEXT NOT NULL
            )"""
        )
        db.execSQL(
            """CREATE TABLE fit_observation_provenance (
                sample_id TEXT PRIMARY KEY NOT NULL,
                device_id TEXT NOT NULL,
                metric TEXT NOT NULL,
                measured_at_ms INTEGER NOT NULL CHECK(measured_at_ms >= 0),
                source_sha256 TEXT NOT NULL
                    REFERENCES fit_sources(source_sha256) ON DELETE RESTRICT,
                record_ordinal INTEGER NOT NULL CHECK(record_ordinal >= 0),
                global_message_number INTEGER NOT NULL CHECK(global_message_number >= 0),
                field_number INTEGER NOT NULL CHECK(field_number >= 0),
                quality TEXT NOT NULL CHECK(quality = 'PROFILE_VALID'),
                decoder_version TEXT NOT NULL,
                FOREIGN KEY(device_id, metric, measured_at_ms, sample_id)
                    REFERENCES readings(device_id, metric, measured_at_ms, sample_id)
                    ON DELETE CASCADE
            )"""
        )
        db.execSQL("CREATE INDEX fit_provenance_by_source ON fit_observation_provenance(source_sha256, record_ordinal)")
        db.execSQL("CREATE INDEX fit_provenance_by_metric_time ON fit_observation_provenance(metric, measured_at_ms)")
    }

    /**
     * Atomic, idempotent insert + per-device checkpoint. Invalid readings fail
     * the entire batch rather than silently advancing the checkpoint.
     */
    fun commitBatch(deviceId: String, readings: List<RawExportRecord>) {
        require(deviceId.isNotBlank())
        require(readings.all {
            it.deviceId == deviceId && it.metric.isNotBlank() && it.unit.isNotBlank() &&
                it.measuredAtEpochMillis >= 0 && it.value.isFinite() &&
                (it.receivedAtEpochMillis == null || it.receivedAtEpochMillis >= 0)
        }) { "Invalid reading in batch" }
        val db = writableDatabase
        db.beginTransaction()
        try {
            val insert = db.compileStatement(
                """INSERT OR IGNORE INTO readings
                    (device_id, metric, measured_at_ms, value, unit, sample_id, received_at_ms)
                    VALUES (?, ?, ?, ?, ?, ?, ?)"""
            )
            for (reading in readings) {
                insert.clearBindings()
                insert.bindString(1, reading.deviceId)
                insert.bindString(2, reading.metric)
                insert.bindLong(3, reading.measuredAtEpochMillis)
                insert.bindDouble(4, reading.value)
                insert.bindString(5, reading.unit)
                insert.bindString(6, reading.sampleId ?: "")
                if (reading.receivedAtEpochMillis == null) insert.bindNull(7)
                else insert.bindLong(7, reading.receivedAtEpochMillis)
                insert.executeInsert()
            }
            val newest = readings.maxOfOrNull { it.measuredAtEpochMillis }
            if (newest != null) {
                val insertCursor = db.compileStatement(
                    "INSERT OR IGNORE INTO sync_cursors(device_id, last_committed_ms) VALUES (?, ?)"
                )
                insertCursor.bindString(1, deviceId)
                insertCursor.bindLong(2, newest)
                insertCursor.executeInsert()
                val advanceCursor = db.compileStatement(
                    """UPDATE sync_cursors SET last_committed_ms =
                       MAX(last_committed_ms, ?) WHERE device_id = ?"""
                )
                advanceCursor.bindLong(1, newest)
                advanceCursor.bindString(2, deviceId)
                advanceCursor.executeUpdateDelete()
            }
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
    }

    /**
     * Atomically stores one validated FIT source, its importable readings and
     * field-level provenance. Existing v1 readings are preserved and may gain
     * provenance when the corresponding private original is backfilled.
     */
    fun commitFitFile(
        sourceSha256: String,
        fileBytes: Long,
        relativePath: String,
        validatedAtEpochMillis: Long,
        decoderVersion: String,
        profileVersion: String,
        observations: List<FitHistoricalObservations.Observation>,
        deviceKey: String = "cirqa-fit"
    ): Int {
        require(sourceSha256.matches(Regex("[0-9a-f]{64}")))
        require(fileBytes > 0 && validatedAtEpochMillis >= 0)
        require(relativePath == "verified_fit/$sourceSha256.fit")
        require(decoderVersion.isNotBlank() && profileVersion.isNotBlank() && deviceKey.isNotBlank())
        require(observations.all {
            it.sourceFileSha256 == sourceSha256 &&
                it.quality == FitHistoricalObservations.Quality.PROFILE_VALID &&
                it.timestampEpochMillis != null &&
                it.metric.unit == it.unit && it.value.isFinite() &&
                it.idempotencyKey.isNotBlank() &&
                it.recordOrdinal >= 0 && it.globalMessageNumber >= 0 && it.fieldNumber >= 0
        }) { "FIT provenance does not match validated source" }
        require(observations.map { it.idempotencyKey }.distinct().size == observations.size) {
            "Duplicate FIT idempotency key in one file"
        }

        val records = observations.map {
            RawExportRecord(
                deviceId = deviceKey,
                metric = it.metric.name.lowercase(),
                measuredAtEpochMillis = requireNotNull(it.timestampEpochMillis),
                value = it.value,
                unit = it.unit,
                sampleId = it.idempotencyKey,
                receivedAtEpochMillis = validatedAtEpochMillis
            )
        }
        val db = writableDatabase
        db.beginTransaction()
        try {
            db.execSQL(
                """INSERT OR IGNORE INTO fit_sources
                   (source_sha256, file_bytes, relative_path, validated_at_ms, decoder_version, profile_version)
                   VALUES (?, ?, ?, ?, ?, ?)""",
                arrayOf(sourceSha256, fileBytes, relativePath, validatedAtEpochMillis, decoderVersion, profileVersion)
            )
            db.rawQuery(
                """SELECT file_bytes, relative_path, decoder_version, profile_version
                   FROM fit_sources WHERE source_sha256 = ?""",
                arrayOf(sourceSha256)
            ).use { row ->
                check(row.moveToFirst() && row.getLong(0) == fileBytes && row.getString(1) == relativePath &&
                    row.getString(2) == decoderVersion && row.getString(3) == profileVersion) {
                    "Stored FIT source metadata conflicts with content hash"
                }
            }

            val readingInsert = db.compileStatement(
                """INSERT OR IGNORE INTO readings
                   (device_id, metric, measured_at_ms, value, unit, sample_id, received_at_ms)
                   VALUES (?, ?, ?, ?, ?, ?, ?)"""
            )
            val provenanceInsert = db.compileStatement(
                """INSERT OR IGNORE INTO fit_observation_provenance
                   (sample_id, device_id, metric, measured_at_ms, source_sha256, record_ordinal,
                    global_message_number, field_number, quality, decoder_version)
                   VALUES (?, ?, ?, ?, ?, ?, ?, ?, 'PROFILE_VALID', ?)"""
            )
            for ((observation, record) in observations.zip(records)) {
                val sampleId = requireNotNull(record.sampleId)
                val existingProvenance = db.rawQuery(
                    """SELECT device_id, metric, measured_at_ms, source_sha256, record_ordinal,
                              global_message_number, field_number, quality, decoder_version
                       FROM fit_observation_provenance WHERE sample_id = ?""",
                    arrayOf(sampleId)
                ).use { row ->
                    if (!row.moveToFirst()) null else StoredFitProvenance(
                        sampleId = sampleId,
                        deviceId = row.getString(0),
                        metric = row.getString(1),
                        measuredAtEpochMillis = row.getLong(2),
                        sourceSha256 = row.getString(3),
                        recordOrdinal = row.getInt(4),
                        globalMessageNumber = row.getInt(5),
                        fieldNumber = row.getInt(6),
                        quality = row.getString(7),
                        decoderVersion = row.getString(8)
                    )
                }
                if (existingProvenance != null) {
                    check(existingProvenance == StoredFitProvenance(
                        sampleId, record.deviceId, record.metric, record.measuredAtEpochMillis,
                        sourceSha256, observation.recordOrdinal, observation.globalMessageNumber,
                        observation.fieldNumber, observation.quality.name, decoderVersion
                    )) { "FIT decoder output conflicts with previously committed provenance" }
                }
                db.rawQuery(
                    """SELECT value, unit FROM readings
                       WHERE device_id = ? AND metric = ? AND measured_at_ms = ? AND sample_id = ?""",
                    arrayOf(record.deviceId, record.metric, record.measuredAtEpochMillis.toString(), sampleId)
                ).use { row ->
                    if (row.moveToFirst()) {
                        check(row.getDouble(0).toBits() == record.value.toBits() && row.getString(1) == record.unit) {
                            "FIT decoder output conflicts with previously committed reading"
                        }
                    }
                }

                readingInsert.clearBindings()
                readingInsert.bindString(1, record.deviceId)
                readingInsert.bindString(2, record.metric)
                readingInsert.bindLong(3, record.measuredAtEpochMillis)
                readingInsert.bindDouble(4, record.value)
                readingInsert.bindString(5, record.unit)
                readingInsert.bindString(6, sampleId)
                readingInsert.bindLong(7, validatedAtEpochMillis)
                readingInsert.executeInsert()

                provenanceInsert.clearBindings()
                provenanceInsert.bindString(1, observation.idempotencyKey)
                provenanceInsert.bindString(2, record.deviceId)
                provenanceInsert.bindString(3, record.metric)
                provenanceInsert.bindLong(4, record.measuredAtEpochMillis)
                provenanceInsert.bindString(5, sourceSha256)
                provenanceInsert.bindLong(6, observation.recordOrdinal.toLong())
                provenanceInsert.bindLong(7, observation.globalMessageNumber.toLong())
                provenanceInsert.bindLong(8, observation.fieldNumber.toLong())
                provenanceInsert.bindString(9, decoderVersion)
                provenanceInsert.executeInsert()
            }

            val newest = records.maxOfOrNull { it.measuredAtEpochMillis }
            if (newest != null) {
                db.execSQL(
                    "INSERT OR IGNORE INTO sync_cursors(device_id, last_committed_ms) VALUES (?, ?)",
                    arrayOf(deviceKey, newest)
                )
                db.execSQL(
                    "UPDATE sync_cursors SET last_committed_ms = MAX(last_committed_ms, ?) WHERE device_id = ?",
                    arrayOf(newest, deviceKey)
                )
            }
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
        return records.size
    }

    fun cursorFor(deviceId: String): SyncCursor {
        require(deviceId.isNotBlank())
        readableDatabase.rawQuery(
            "SELECT last_committed_ms FROM sync_cursors WHERE device_id = ?",
            arrayOf(deviceId)
        ).use { cursor ->
            return if (cursor.moveToFirst()) SyncCursor(cursor.getLong(0)) else SyncCursor()
        }
    }

    /** Query only requested measurement times, with a stable ordering. */
    fun readWindow(window: ExportWindow): List<RawExportRecord> {
        val result = mutableListOf<RawExportRecord>()
        readableDatabase.rawQuery(
            """SELECT device_id, metric, measured_at_ms, value, unit, sample_id, received_at_ms
               FROM readings WHERE measured_at_ms >= ? AND measured_at_ms < ?
               ORDER BY measured_at_ms, device_id, metric, sample_id""",
            arrayOf(window.startInclusiveEpochMillis.toString(), window.endExclusiveEpochMillis.toString())
        ).use { cursor ->
            while (cursor.moveToNext()) {
                result += RawExportRecord(
                    deviceId = cursor.getString(0),
                    metric = cursor.getString(1),
                    measuredAtEpochMillis = cursor.getLong(2),
                    value = cursor.getDouble(3),
                    unit = cursor.getString(4),
                    sampleId = cursor.getString(5).ifEmpty { null },
                    receivedAtEpochMillis = if (cursor.isNull(6)) null else cursor.getLong(6)
                )
            }
        }
        return result
    }
}
