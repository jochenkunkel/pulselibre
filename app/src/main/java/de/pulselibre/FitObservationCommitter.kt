package de.pulselibre

import android.database.sqlite.SQLiteDatabase

/** Shared atomic FIT writer for the historical transition store and canonical store. */
internal object FitObservationCommitter {
    fun commit(
        db: SQLiteDatabase,
        readingsTable: String,
        sourceSha256: String,
        fileBytes: Long,
        relativePath: String,
        validatedAtEpochMillis: Long,
        decoderVersion: String,
        profileVersion: String,
        observations: List<FitHistoricalObservations.Observation>,
        deviceKey: String
    ): Int {
        require(readingsTable == "readings" || readingsTable == "observations")
        require(sourceSha256.matches(Regex("[0-9a-f]{64}")))
        require(fileBytes > 0 && validatedAtEpochMillis >= 0)
        require(relativePath == "verified_fit/$sourceSha256.fit")
        require(decoderVersion.isNotBlank() && profileVersion.isNotBlank() && deviceKey.isNotBlank())
        require(observations.all {
            it.sourceFileSha256 == sourceSha256 &&
                it.quality == FitHistoricalObservations.Quality.PROFILE_VALID &&
                it.timestampEpochMillis != null && it.metric.unit == it.unit &&
                it.value.isFinite() && it.idempotencyKey.isNotBlank() &&
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
                """INSERT OR IGNORE INTO $readingsTable
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
                    """SELECT value, unit FROM $readingsTable
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
                provenanceInsert.bindString(1, sampleId)
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
                    """INSERT INTO sync_cursors(device_id, last_committed_ms) VALUES (?, ?)
                       ON CONFLICT(device_id) DO UPDATE SET
                       last_committed_ms = MAX(last_committed_ms, excluded.last_committed_ms)""",
                    arrayOf(deviceKey, newest)
                )
            }
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
        return records.size
    }
}
