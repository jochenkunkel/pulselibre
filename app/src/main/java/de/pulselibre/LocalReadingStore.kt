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

data class StoredFitReviewCandidate(
    val sampleId: String,
    val deviceId: String,
    val metric: String,
    val measuredAtEpochMillis: Long?,
    val value: Double,
    val unit: String,
    val sourceSha256: String,
    val recordOrdinal: Int,
    val globalMessageNumber: Int,
    val fieldNumber: Int,
    val quality: String,
    val timestampOrigin: String,
    val decoderVersion: String
)

enum class StoredFitFieldKind { STANDARD, DEVELOPER }

data class FitFieldInventoryRecord(
    val sourceSha256: String,
    val globalMessageNumber: Int,
    val kind: StoredFitFieldKind,
    val fieldNumber: Int,
    val baseTypeCode: Int?,
    val fieldSize: Int,
    val developerDataIndex: Int?,
    val classification: String,
    val definitionCount: Long,
    val observedMessageCount: Long,
    val validElementCount: Long?,
    val invalidElementCount: Long?,
    val decoderVersion: String,
    val profileVersion: String,
    val status: FitFieldInventoryStatus = FitFieldInventoryStatus.UNREVIEWED
)

enum class FitFieldInventoryStatus { UNREVIEWED, INVESTIGATING, SUPPORTED, IGNORED }

typealias StoredFitUnmappedField = FitFieldInventoryRecord

interface FitObservationStore {
    fun commitFitFile(
        sourceSha256: String,
        fileBytes: Long,
        relativePath: String,
        validatedAtEpochMillis: Long,
        decoderVersion: String,
        profileVersion: String,
        observations: List<FitHistoricalObservations.Observation>,
        deviceMetadata: List<FitHistoricalObservations.DeviceMetadata> = emptyList(),
        unmappedFields: List<FitFieldInventoryRecord> = emptyList(),
        deviceKey: String = "cirqa-fit",
        reviewCandidates: List<FitHistoricalObservations.ReviewCandidate> = emptyList(),
        decodedFields: List<FitHistoricalObservations.DecodedField> = emptyList()
    ): Int
}

/**
 * Private, offline SQLite store. The database contains only validated readings.
 * Each batch and its cursor are committed in one transaction.
 * No Bluetooth pairing, network access or Garmin-specific decoding occurs here.
 */
class LocalReadingStore(context: Context) : SQLiteOpenHelper(
    context.applicationContext, "pulselibre_readings.db", null, 3
), FitObservationStore {
    init { SqliteOpen.skipLocale(this) }

    override fun onConfigure(db: SQLiteDatabase) {
        super.onConfigure(db)
        db.setForeignKeyConstraintsEnabled(true)
    }

    override fun onCreate(db: SQLiteDatabase) {
        createReadingsSchema(db)
        createFitProvenanceSchema(db)
        createFitReviewCandidateSchema(db)
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
        require(oldVersion in 1..2 && newVersion == 3) {
            "Missing database migration from $oldVersion to $newVersion"
        }
        // Additive only: existing readings and cursors remain byte-for-byte intact.
        if (oldVersion == 1) createFitProvenanceSchema(db)
        createFitReviewCandidateSchema(db)
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
        reviewCandidates: List<FitHistoricalObservations.ReviewCandidate>,
        decodedFields: List<FitHistoricalObservations.DecodedField>
    ): Int = FitObservationCommitter.commit(
        writableDatabase, "readings", sourceSha256, fileBytes, relativePath, validatedAtEpochMillis,
        decoderVersion, profileVersion, observations, deviceMetadata, unmappedFields, deviceKey,
        reviewCandidates, decodedFields
    )

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
