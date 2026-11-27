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
        deviceMetadata: List<FitHistoricalObservations.DeviceMetadata>,
        unmappedFields: List<FitFieldInventoryRecord>,
        deviceKey: String,
        reviewCandidates: List<FitHistoricalObservations.ReviewCandidate> = emptyList(),
        decodedFields: List<FitHistoricalObservations.DecodedField> = emptyList()
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
        require(reviewCandidates.all {
            it.sourceFileSha256 == sourceSha256 && it.value.isFinite() &&
                it.idempotencyKey.isNotBlank() && it.recordOrdinal > 0 &&
                it.globalMessageNumber >= 0 && it.fieldNumber >= 0 &&
                (it.timestampEpochMillis == null || it.timestampEpochMillis >= 0) &&
                it.quality == "UNTRUSTED_PROFILE_CANDIDATE" &&
                it.decoderVersion.matches(Regex("[0-9]{1,8}"))
        }) { "FIT review candidate does not match validated source" }
        require(reviewCandidates.map { it.idempotencyKey }.distinct().size == reviewCandidates.size) {
            "Duplicate FIT review candidate idempotency key in one file"
        }
        require(deviceMetadata.all {
            it.sourceFileSha256 == sourceSha256 && it.recordOrdinal > 0 &&
                (it.timestampEpochMillis == null || it.timestampEpochMillis >= 0) &&
                (it.deviceTypeCode == null || it.deviceTypeCode in 0..254) &&
                (it.manufacturerCode == null || it.manufacturerCode in 0..65534) &&
                (it.productCode == null || it.productCode in 0..65534) &&
                (it.softwareVersion == null || it.softwareVersion.isFinite() && it.softwareVersion in 0.0..655.34) &&
                (it.batteryVoltageVolts == null || it.batteryVoltageVolts.isFinite() && it.batteryVoltageVolts in 0.0..20.0) &&
                (it.batteryStatusCode == null || it.batteryStatusCode in 0..254) &&
                (it.batteryLevelPercent == null || it.batteryLevelPercent in 0..100)
        }) { "FIT device metadata does not match validated source" }
        require(deviceMetadata.map { it.recordOrdinal }.distinct().size == deviceMetadata.size) {
            "Duplicate FIT device_info ordinal in one file"
        }
        require(unmappedFields.all {
            it.sourceSha256 == sourceSha256 &&
                it.globalMessageNumber in 0..65535 &&
                it.fieldNumber in 0..255 && it.fieldSize in 1..255 &&
                (it.baseTypeCode == null || it.baseTypeCode in 0..255) &&
                (it.developerDataIndex == null || it.developerDataIndex in 0..255) &&
                it.definitionCount in 0..1_000_000 &&
                it.observedMessageCount in 0..1_000_000 &&
                (it.validElementCount == null || it.validElementCount in 0..1_000_000) &&
                (it.invalidElementCount == null || it.invalidElementCount in 0..1_000_000)
        }) { "FIT field inventory does not match validated source or limits" }
        require(decodedFields.size <= FitHistoricalObservations.MAX_DECODED_FIELDS &&
            decodedFields.all {
                it.sourceFileSha256 == sourceSha256 && it.recordOrdinal > 0 &&
                    it.globalMessageNumber in 0..65535 && it.fieldNumber in 0..255 &&
                    it.fieldSize in 1..255 && it.rawBytes.size == it.fieldSize &&
                    (it.baseTypeCode == null || it.baseTypeCode in 0..255) &&
                    (it.developerDataIndex == null || it.developerDataIndex in 0..255) &&
                    (it.timestampEpochMillis == null || it.timestampEpochMillis >= 0)
            }) { "FIT decoded fields do not match validated source or limits" }

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
                arrayOf<Any?>(
                    sourceSha256, fileBytes, relativePath, validatedAtEpochMillis, decoderVersion, profileVersion
                )
            )
            db.rawQuery(
                """SELECT file_bytes, relative_path
                   FROM fit_sources WHERE source_sha256 = ?""",
                arrayOf(sourceSha256)
            ).use { row ->
                check(row.moveToFirst() && row.getLong(0) == fileBytes && row.getString(1) == relativePath) {
                    "Stored FIT source metadata conflicts with content hash"
                }
            }
            val candidateInsert = db.compileStatement(
                """INSERT OR IGNORE INTO fit_review_candidates
                   (sample_id, device_id, metric, measured_at_ms, value, unit, source_sha256,
                    record_ordinal, global_message_number, field_number, quality, timestamp_origin,
                    decoder_version)
                   VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)"""
            )
            for (candidate in reviewCandidates) {
                val metric = when (candidate.profileFieldName) {
                    "reading_spo2" -> "spo2"
                    "reading_confidence" -> "spo2_confidence"
                    "spo2_measurement_mode" -> "spo2_measurement_mode"
                    "stress_level_value" -> "stress"
                    "respiration_rate" -> "respiration"
                    "sleep_level" -> "sleep_level"
                    "monitoring_cycles" -> "monitoring_cycles"
                    "monitoring_activity_type" -> "monitoring_activity_type"
                    MonitoringActivityIntensity.ACTIVITY_TYPE -> "monitoring_current_activity_type"
                    MonitoringActivityIntensity.INTENSITY -> "monitoring_intensity_code"
                    "average_deviation" -> "skin_temperature_deviation"
                    "average_7_day_deviation" -> "skin_temperature_7_day_deviation"
                    else -> FitHistoricalObservations.profileScalarMetric(candidate.profileFieldName)
                        ?.name?.lowercase() ?: error("Unsupported FIT review candidate")
                }
                candidateInsert.clearBindings()
                candidateInsert.bindString(1, candidate.idempotencyKey)
                candidateInsert.bindString(2, deviceKey)
                candidateInsert.bindString(3, metric)
                bindNullableLong(candidateInsert, 4, candidate.timestampEpochMillis)
                candidateInsert.bindDouble(5, candidate.value)
                candidateInsert.bindString(6, candidate.unit)
                candidateInsert.bindString(7, sourceSha256)
                candidateInsert.bindLong(8, candidate.recordOrdinal.toLong())
                candidateInsert.bindLong(9, candidate.globalMessageNumber.toLong())
                candidateInsert.bindLong(10, candidate.fieldNumber.toLong())
                candidateInsert.bindString(11, candidate.quality)
                candidateInsert.bindString(12, candidate.timestampOrigin.name)
                candidateInsert.bindString(13, candidate.decoderVersion)
                candidateInsert.executeInsert()
                db.rawQuery(
                    """SELECT device_id, metric, measured_at_ms, value, unit, source_sha256,
                              record_ordinal, global_message_number, field_number, quality,
                              timestamp_origin, decoder_version
                       FROM fit_review_candidates WHERE sample_id = ?""",
                    arrayOf(candidate.idempotencyKey)
                ).use { row ->
                    check(row.moveToFirst()) { "FIT review candidate insert was not retained" }
                    if (row.getString(11) == candidate.decoderVersion) {
                        check(row.getString(0) == deviceKey && row.getString(1) == metric &&
                            sameNullableLong(row, 2, candidate.timestampEpochMillis) &&
                            row.getDouble(3).toBits() == candidate.value.toBits() &&
                            row.getString(4) == candidate.unit && row.getString(5) == sourceSha256 &&
                            row.getInt(6) == candidate.recordOrdinal &&
                            row.getInt(7) == candidate.globalMessageNumber &&
                            row.getInt(8) == candidate.fieldNumber && row.getString(9) == candidate.quality &&
                            row.getString(10) == candidate.timestampOrigin.name) {
                            "FIT review candidate conflicts within one decoder version"
                        }
                    }
                }
            }

            if (readingsTable == "observations") {
                val inventoryInsert = db.compileStatement(
                    """INSERT INTO fit_unmapped_fields
                       (source_sha256, global_message_number, field_kind, field_number,
                        base_type_code, field_size, developer_data_index, classification,
                        definition_count, observed_message_count, valid_element_count,
                        invalid_element_count, decoder_version, profile_version)
                       VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                       ON CONFLICT(source_sha256, global_message_number, field_kind, field_number,
                                   base_type_code, field_size, developer_data_index, classification)
                       DO UPDATE SET definition_count = excluded.definition_count,
                           observed_message_count = excluded.observed_message_count,
                           valid_element_count = excluded.valid_element_count,
                           invalid_element_count = excluded.invalid_element_count,
                           decoder_version = excluded.decoder_version,
                           profile_version = excluded.profile_version"""
                )
                for (field in unmappedFields) {
                    inventoryInsert.clearBindings()
                    inventoryInsert.bindString(1, sourceSha256)
                    inventoryInsert.bindLong(2, field.globalMessageNumber.toLong())
                    inventoryInsert.bindString(3, field.kind.name)
                    inventoryInsert.bindLong(4, field.fieldNumber.toLong())
                    inventoryInsert.bindLong(5, (field.baseTypeCode ?: -1).toLong())
                    inventoryInsert.bindLong(6, field.fieldSize.toLong())
                    inventoryInsert.bindLong(7, (field.developerDataIndex ?: -1).toLong())
                    inventoryInsert.bindString(8, field.classification)
                    inventoryInsert.bindLong(9, field.definitionCount)
                    inventoryInsert.bindLong(10, field.observedMessageCount)
                    bindNullableLong(inventoryInsert, 11, field.validElementCount)
                    bindNullableLong(inventoryInsert, 12, field.invalidElementCount)
                    inventoryInsert.bindString(13, decoderVersion)
                    inventoryInsert.bindString(14, profileVersion)
                    inventoryInsert.executeInsert()
                }
                val metadataInsert = db.compileStatement(
                    """INSERT OR IGNORE INTO fit_device_metadata
                       (source_sha256, record_ordinal, observed_at_ms, device_type_code,
                        manufacturer_code, product_code, software_version, battery_voltage_volts,
                        battery_status_code, battery_level_percent)
                       VALUES(?, ?, ?, ?, ?, ?, ?, ?, ?, ?)"""
                )
                for (metadata in deviceMetadata) {
                    metadataInsert.clearBindings()
                    metadataInsert.bindString(1, sourceSha256)
                    metadataInsert.bindLong(2, metadata.recordOrdinal.toLong())
                    bindNullableLong(metadataInsert, 3, metadata.timestampEpochMillis)
                    bindNullableInt(metadataInsert, 4, metadata.deviceTypeCode)
                    bindNullableInt(metadataInsert, 5, metadata.manufacturerCode)
                    bindNullableInt(metadataInsert, 6, metadata.productCode)
                    bindNullableDouble(metadataInsert, 7, metadata.softwareVersion)
                    bindNullableDouble(metadataInsert, 8, metadata.batteryVoltageVolts)
                    bindNullableInt(metadataInsert, 9, metadata.batteryStatusCode)
                    bindNullableInt(metadataInsert, 10, metadata.batteryLevelPercent)
                    metadataInsert.executeInsert()

                    db.rawQuery(
                        """SELECT observed_at_ms, device_type_code, manufacturer_code, product_code,
                                  software_version, battery_voltage_volts, battery_status_code, battery_level_percent
                           FROM fit_device_metadata WHERE source_sha256 = ? AND record_ordinal = ?""",
                        arrayOf(sourceSha256, metadata.recordOrdinal.toString())
                    ).use { row ->
                        check(row.moveToFirst() &&
                            sameNullableLong(row, 0, metadata.timestampEpochMillis) &&
                            sameNullableInt(row, 1, metadata.deviceTypeCode) &&
                            sameNullableInt(row, 2, metadata.manufacturerCode) &&
                            sameNullableInt(row, 3, metadata.productCode) &&
                            sameNullableDouble(row, 4, metadata.softwareVersion) &&
                            sameNullableDouble(row, 5, metadata.batteryVoltageVolts) &&
                            sameNullableInt(row, 6, metadata.batteryStatusCode) &&
                            sameNullableInt(row, 7, metadata.batteryLevelPercent)) {
                            "FIT device metadata conflicts with previously committed source"
                        }
                    }
                }
            } else {
                require(deviceMetadata.isEmpty()) {
                    "Historical transition store does not accept FIT device metadata"
                }
                require(unmappedFields.isEmpty()) {
                    "Historical transition store does not accept FIT field inventories"
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
                    // Retain the original decoder provenance for an unchanged sample.
                    // A later generation may reproduce it without rewriting history.
                    check(existingProvenance.copy(decoderVersion = decoderVersion) == StoredFitProvenance(
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

            if (readingsTable == "observations") {
                persistDecoderGeneration(
                    db = db,
                    sourceSha256 = sourceSha256,
                    decoderVersion = decoderVersion,
                    profileVersion = profileVersion,
                    completedAtEpochMillis = validatedAtEpochMillis,
                    observations = observations,
                    records = records,
                    decodedFields = decodedFields
                )
            }

            val newest = records.maxOfOrNull { it.measuredAtEpochMillis }
            if (newest != null) {
                db.execSQL(
                    """INSERT INTO sync_cursors(device_id, last_committed_ms) VALUES (?, ?)
                       ON CONFLICT(device_id) DO UPDATE SET
                       last_committed_ms = MAX(last_committed_ms, excluded.last_committed_ms)""",
                    arrayOf<Any?>(deviceKey, newest)
                )
            }
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
        TrendCache.invalidate() // new FIT data: cached trends are outdated (A6-33)
        return records.size
    }

    /**
     * Store the exact canonical decoder output as an immutable generation.
     * The first complete generation becomes active; later replay generations
     * remain inactive until a caller explicitly activates a validated output.
     */
    internal fun persistDecoderGeneration(
        db: SQLiteDatabase,
        sourceSha256: String,
        decoderVersion: String,
        profileVersion: String,
        completedAtEpochMillis: Long,
        observations: List<FitHistoricalObservations.Observation>,
        records: List<RawExportRecord>,
        decodedFields: List<FitHistoricalObservations.DecodedField> = emptyList()
    ) {
        val generationId = FitDecoderGeneration.id(sourceSha256, decoderVersion, profileVersion)
        val generationRows = observations.zip(records).map { (observation, record) ->
            StoredFitGenerationObservation(
                generationId = generationId,
                sampleId = requireNotNull(record.sampleId),
                deviceId = record.deviceId,
                metric = record.metric,
                measuredAtEpochMillis = record.measuredAtEpochMillis,
                value = record.value,
                unit = record.unit,
                receivedAtEpochMillis = record.receivedAtEpochMillis,
                recordOrdinal = observation.recordOrdinal,
                globalMessageNumber = observation.globalMessageNumber,
                fieldNumber = observation.fieldNumber,
                quality = observation.quality.name,
                timestampOrigin = observation.timestampOrigin.name
            )
        }
        val fieldRows = decodedFields.map { field ->
            StoredFitGenerationField(
                generationId, field.recordOrdinal, field.globalMessageNumber, field.fieldKind,
                field.fieldNumber, field.baseTypeCode, field.fieldSize, field.developerDataIndex,
                field.bigEndian, field.rawBytes, field.timestampEpochMillis,
                field.timestampOrigin.name
            )
        }
        val outputDigest = FitDecoderGeneration.outputDigest(generationRows, fieldRows)
        db.execSQL(
            """INSERT OR IGNORE INTO fit_decoder_generations
               (generation_id, source_sha256, decoder_version, profile_version,
                 output_digest, output_count, decoded_field_count, completed_at_ms)
                VALUES(?, ?, ?, ?, ?, ?, ?, ?)""",
            arrayOf<Any?>(
                generationId, sourceSha256, decoderVersion, profileVersion, outputDigest,
                generationRows.size, fieldRows.size, completedAtEpochMillis
            )
        )
        db.rawQuery(
             """SELECT source_sha256, decoder_version, profile_version, output_digest, output_count,
                       decoded_field_count
               FROM fit_decoder_generations WHERE generation_id = ?""",
            arrayOf(generationId)
        ).use { row ->
            check(row.moveToFirst() && row.getString(0) == sourceSha256 &&
                row.getString(1) == decoderVersion && row.getString(2) == profileVersion &&
                row.getString(3) == outputDigest && row.getLong(4) == generationRows.size.toLong() &&
                row.getLong(5) == fieldRows.size.toLong()) {
                "FIT decoder output conflicts with immutable generation identity"
            }
        }

        val insert = db.compileStatement(
            """INSERT OR IGNORE INTO fit_generation_observations
               (generation_id, sample_id, device_id, metric, measured_at_ms, value, unit,
                received_at_ms, record_ordinal, global_message_number, field_number, quality,
                timestamp_origin)
               VALUES(?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)"""
        )
        generationRows.forEach { row ->
            insert.clearBindings()
            insert.bindString(1, row.generationId)
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
            insert.bindString(13, row.timestampOrigin)
            insert.executeInsert()
            db.rawQuery(
                """SELECT device_id, metric, measured_at_ms, value, unit, received_at_ms,
                          record_ordinal, global_message_number, field_number, quality, timestamp_origin
                   FROM fit_generation_observations WHERE generation_id = ? AND sample_id = ?""",
                arrayOf(generationId, row.sampleId)
            ).use { stored ->
                check(stored.moveToFirst() &&
                    stored.getString(0) == row.deviceId && stored.getString(1) == row.metric &&
                    stored.getLong(2) == row.measuredAtEpochMillis &&
                    stored.getDouble(3).toBits() == row.value.toBits() &&
                    stored.getString(4) == row.unit &&
                    stored.getInt(6) == row.recordOrdinal &&
                    stored.getInt(7) == row.globalMessageNumber &&
                    stored.getInt(8) == row.fieldNumber &&
                    stored.getString(9) == row.quality &&
                    stored.getString(10) == row.timestampOrigin) {
                    "FIT output conflicts with previously stored immutable generation"
                }
            }
        }
        val fieldInsert = db.compileStatement(
            """INSERT OR IGNORE INTO fit_generation_fields
               (generation_id, record_ordinal, global_message_number, field_kind, field_number,
                base_type_code, field_size, developer_data_index, big_endian, raw_bytes,
                measured_at_ms, timestamp_origin)
               VALUES(?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)"""
        )
        for (field in fieldRows) {
            fieldInsert.clearBindings()
            fieldInsert.bindString(1, field.generationId)
            fieldInsert.bindLong(2, field.recordOrdinal.toLong())
            fieldInsert.bindLong(3, field.globalMessageNumber.toLong())
            fieldInsert.bindString(4, field.fieldKind)
            fieldInsert.bindLong(5, field.fieldNumber.toLong())
            if (field.baseTypeCode == null) fieldInsert.bindNull(6) else fieldInsert.bindLong(6, field.baseTypeCode.toLong())
            fieldInsert.bindLong(7, field.fieldSize.toLong())
            fieldInsert.bindLong(8, (field.developerDataIndex ?: -1).toLong())
            fieldInsert.bindLong(9, if (field.bigEndian) 1 else 0)
            fieldInsert.bindBlob(10, field.rawBytes)
            if (field.measuredAtEpochMillis == null) fieldInsert.bindNull(11) else fieldInsert.bindLong(11, field.measuredAtEpochMillis)
            fieldInsert.bindString(12, field.timestampOrigin)
            fieldInsert.executeInsert()
            val same = db.rawQuery(
                """SELECT base_type_code, field_size, big_endian, raw_bytes, measured_at_ms,
                          timestamp_origin FROM fit_generation_fields
                   WHERE generation_id=? AND record_ordinal=? AND field_kind=? AND field_number=?
                     AND developer_data_index = ?""",
                arrayOf(field.generationId, field.recordOrdinal.toString(), field.fieldKind,
                    field.fieldNumber.toString(), (field.developerDataIndex ?: -1).toString())
            ).use { row ->
                row.moveToFirst() &&
                    (if (field.baseTypeCode == null) row.isNull(0) else !row.isNull(0) && row.getInt(0) == field.baseTypeCode) &&
                    row.getInt(1) == field.fieldSize && (row.getInt(2) != 0) == field.bigEndian &&
                    row.getBlob(3).contentEquals(field.rawBytes) &&
                    (if (field.measuredAtEpochMillis == null) row.isNull(4) else !row.isNull(4) && row.getLong(4) == field.measuredAtEpochMillis) &&
                    row.getString(5) == field.timestampOrigin
            }
            check(same) { "FIT raw field conflicts with immutable decoder generation" }
        }
        db.rawQuery(
            "SELECT COUNT(*) FROM fit_generation_fields WHERE generation_id = ?",
            arrayOf(generationId)
        ).use { row ->
            check(row.moveToFirst() && row.getLong(0) == fieldRows.size.toLong()) {
                "FIT raw field generation is incomplete"
            }
        }
        db.execSQL(
            """INSERT OR IGNORE INTO fit_active_generations(source_sha256, generation_id)
               VALUES(?, ?)""",
            arrayOf(sourceSha256, generationId)
        )
        db.rawQuery(
            "SELECT generation_id FROM fit_active_generations WHERE source_sha256 = ?",
            arrayOf(sourceSha256)
        ).use { active ->
            check(active.moveToFirst()) { "FIT source has no active complete generation" }
            // Deliberately do not replace another active generation implicitly.
        }
    }

    private fun bindNullableLong(statement: android.database.sqlite.SQLiteStatement, index: Int, value: Long?) {
        if (value == null) statement.bindNull(index) else statement.bindLong(index, value)
    }

    private fun bindNullableInt(statement: android.database.sqlite.SQLiteStatement, index: Int, value: Int?) {
        if (value == null) statement.bindNull(index) else statement.bindLong(index, value.toLong())
    }

    private fun bindNullableDouble(statement: android.database.sqlite.SQLiteStatement, index: Int, value: Double?) {
        if (value == null) statement.bindNull(index) else statement.bindDouble(index, value)
    }

    private fun sameNullableLong(cursor: android.database.Cursor, index: Int, value: Long?): Boolean =
        if (value == null) cursor.isNull(index) else !cursor.isNull(index) && cursor.getLong(index) == value

    private fun sameNullableInt(cursor: android.database.Cursor, index: Int, value: Int?): Boolean =
        if (value == null) cursor.isNull(index) else !cursor.isNull(index) && cursor.getInt(index) == value

    private fun sameNullableDouble(cursor: android.database.Cursor, index: Int, value: Double?): Boolean =
        if (value == null) cursor.isNull(index) else
            !cursor.isNull(index) && cursor.getDouble(index).toBits() == value.toBits()
}
