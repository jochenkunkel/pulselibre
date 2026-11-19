package de.pulselibre

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import org.json.JSONObject
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.BufferedReader
import java.io.BufferedWriter
import java.io.Closeable
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.InputStream
import java.io.OutputStream
import java.io.OutputStreamWriter
import java.security.DigestInputStream
import java.security.DigestOutputStream
import java.security.MessageDigest
import java.time.Instant
import java.util.UUID
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

private const val BACKUP_FORMAT = "de.pulselibre.local-database-backup"
private const val BACKUP_VERSION = 6
private const val MAX_ARCHIVE_BYTES = 128L * 1024L * 1024L
private const val MAX_ENTRY_BYTES = 120L * 1024L * 1024L
private const val MAX_LINE_BYTES = 2 * 1024 * 1024
private const val MAX_RECORDS = 1_000_000L
private const val MAX_MANIFEST_BYTES = 64 * 1024
private const val EVENTS_ENTRY = "cirqa_events.jsonl"
private const val READINGS_ENTRY = "legacy_readings.jsonl"
private const val CURSORS_ENTRY = "sync_cursors.jsonl"
private const val FIT_SOURCES_ENTRY = "fit_sources.jsonl"
private const val FIT_PROVENANCE_ENTRY = "fit_observation_provenance.jsonl"
private const val CANONICAL_READINGS_ENTRY = "canonical_observations.jsonl"
private const val CANONICAL_CURSORS_ENTRY = "canonical_sync_cursors.jsonl"
private const val CANONICAL_FIT_SOURCES_ENTRY = "canonical_fit_sources.jsonl"
private const val CANONICAL_FIT_PROVENANCE_ENTRY = "canonical_fit_observation_provenance.jsonl"
private const val CANONICAL_DEVICE_METADATA_ENTRY = "canonical_fit_device_metadata.jsonl"
private const val RAW_DEVICE_METADATA_ENTRY = "raw_device_metadata.jsonl"
private const val CANONICAL_UNMAPPED_FIT_FIELDS_ENTRY = "canonical_fit_unmapped_fields.jsonl"
private const val MANIFEST_ENTRY = "manifest.json"
private val BACKUP_V1_ENTRIES = setOf(EVENTS_ENTRY, READINGS_ENTRY, CURSORS_ENTRY)
private val BACKUP_V2_ENTRIES =
    BACKUP_V1_ENTRIES + setOf(FIT_SOURCES_ENTRY, FIT_PROVENANCE_ENTRY)
private val BACKUP_V3_ENTRIES = BACKUP_V2_ENTRIES + setOf(
    CANONICAL_READINGS_ENTRY, CANONICAL_CURSORS_ENTRY,
    CANONICAL_FIT_SOURCES_ENTRY, CANONICAL_FIT_PROVENANCE_ENTRY
)
private val BACKUP_V4_ENTRIES = BACKUP_V3_ENTRIES + CANONICAL_DEVICE_METADATA_ENTRY
private val BACKUP_V5_ENTRIES = BACKUP_V4_ENTRIES + RAW_DEVICE_METADATA_ENTRY
private val BACKUP_V6_ENTRIES = BACKUP_V5_ENTRIES + CANONICAL_UNMAPPED_FIT_FIELDS_ENTRY

data class DatabaseBackupReport(
    val createdAt: String,
    val events: Long,
    val legacyReadings: Long,
    val syncCursors: Long,
    val fitSources: Long = 0,
    val fitProvenance: Long = 0,
    val canonicalReadings: Long = 0,
    val canonicalSyncCursors: Long = 0,
    val canonicalFitSources: Long = 0,
    val canonicalFitProvenance: Long = 0,
    val canonicalDeviceMetadata: Long = 0,
    val rawDeviceMetadata: Long = 0
)

/** A validated backup staged privately before any local database can be changed. */
class PreparedDatabaseRestore internal constructor(
    internal val directory: File,
    val createdAt: String,
    val events: Long,
    val legacyReadings: Long,
    val syncCursors: Long,
    internal val version: Int,
    val fitSources: Long,
    val fitProvenance: Long,
    val canonicalReadings: Long,
    val canonicalSyncCursors: Long,
    val canonicalFitSources: Long,
    val canonicalFitProvenance: Long,
    val canonicalDeviceMetadata: Long,
    val rawDeviceMetadata: Long
) : Closeable {
    override fun close() { directory.deleteRecursively() }
}

/** Versioned, checksummed archive for both local SQLite stores. The archive is not encrypted. */
object DatabaseBackup {
    /** Stream a consistent snapshot into a temporary archive, then copy it to the chosen SAF file. */
    fun write(context: Context, destination: OutputStream): DatabaseBackupReport {
        val temp = File.createTempFile("pulselibre-backup-", ".zip", context.cacheDir)
        try {
            val report = writeArchive(context, temp)
            BufferedInputStream(FileInputStream(temp)).use { source ->
                val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                var copied = 0L
                while (true) {
                    val count = source.read(buffer)
                    if (count < 0) break
                    copied += count
                    require(copied <= MAX_ARCHIVE_BYTES) { "Backup-Datei überschreitet das Größenlimit" }
                    destination.write(buffer, 0, count)
                }
                destination.flush()
            }
            return report
        } finally {
            temp.delete()
        }
    }

    /** Read, bound, checksum and validate the complete archive before showing overwrite confirmation. */
    fun prepareRestore(context: Context, source: InputStream): PreparedDatabaseRestore {
        val directory = File(context.cacheDir, "database-restore-${UUID.randomUUID()}")
        require(directory.mkdir()) { "Temporärer Wiederherstellungsbereich kann nicht erstellt werden" }
        try {
            val extracted = extractArchive(source, directory)
            val manifestFile = extracted.getValue(MANIFEST_ENTRY)
            val manifestBytes = manifestFile.readBytes()
            require(manifestBytes.size <= MAX_MANIFEST_BYTES) { "Backup-Manifest ist zu groß" }
            val manifest = JSONObject(manifestBytes.toString(Charsets.UTF_8))
            require(manifest.optString("format") == BACKUP_FORMAT) { "Das ist kein PulsLibre-Datenbankbackup" }
            val version = manifest.optInt("version", -1)
            val dataEntries = when (version) {
                1 -> BACKUP_V1_ENTRIES
                2 -> BACKUP_V2_ENTRIES
                3 -> BACKUP_V3_ENTRIES
                4 -> BACKUP_V4_ENTRIES
                5 -> BACKUP_V5_ENTRIES
                BACKUP_VERSION -> BACKUP_V6_ENTRIES
                else -> throw IllegalArgumentException("Backup-Version wird nicht unterstützt")
            }
            require(extracted.keys == dataEntries + MANIFEST_ENTRY) {
                "Backup-Einträge passen nicht zur Versionsnummer"
            }
            require(manifest.optString("app_id") == "de.pulselibre") { "Backup gehört zu einer anderen App" }
            val createdAt = manifest.getString("created_at")
            Instant.parse(createdAt)
            require(manifest.optString("encryption") == "none") { "Unbekannte Backup-Verschlüsselung" }

            val entryMetadata = manifest.getJSONObject("entries")
            require(entryMetadata.length() == dataEntries.size) { "Backup-Metadaten sind unvollständig" }
            val recordCounts = mutableMapOf<String, Long>()
            dataEntries.forEach { name ->
                val file = extracted.getValue(name)
                val metadata = entryMetadata.getJSONObject(name)
                require(sha256(file) == metadata.getString("sha256")) { "Backup-Prüfsumme stimmt nicht: $name" }
                require(file.length() == metadata.getLong("bytes")) { "Backup-Größe stimmt nicht: $name" }
                val count = validateJsonLines(name, file)
                require(count == metadata.getLong("records")) { "Backup-Datensatzanzahl stimmt nicht: $name" }
                recordCounts[name] = count
            }
            if (version >= 2) {
                validateFitReferences(directory, READINGS_ENTRY, FIT_SOURCES_ENTRY, FIT_PROVENANCE_ENTRY)
            }
            if (version >= 3) {
                validateFitReferences(
                    directory, CANONICAL_READINGS_ENTRY, CANONICAL_FIT_SOURCES_ENTRY,
                    CANONICAL_FIT_PROVENANCE_ENTRY
                )
            }
            if (version >= 4) {
                validateDeviceMetadataReferences(directory)
            }
            if (version >= 5) {
                validateRawDeviceMetadataReferences(directory)
            }
            if (version >= 6) validateUnmappedFitFieldReferences(directory)
            return PreparedDatabaseRestore(
                directory = directory,
                createdAt = createdAt,
                events = recordCounts.getValue(EVENTS_ENTRY),
                legacyReadings = recordCounts.getValue(READINGS_ENTRY),
                syncCursors = recordCounts.getValue(CURSORS_ENTRY),
                version = version,
                fitSources = recordCounts[FIT_SOURCES_ENTRY] ?: 0,
                fitProvenance = recordCounts[FIT_PROVENANCE_ENTRY] ?: 0,
                canonicalReadings = recordCounts[CANONICAL_READINGS_ENTRY] ?: 0,
                canonicalSyncCursors = recordCounts[CANONICAL_CURSORS_ENTRY] ?: 0,
                canonicalFitSources = recordCounts[CANONICAL_FIT_SOURCES_ENTRY] ?: 0,
                canonicalFitProvenance = recordCounts[CANONICAL_FIT_PROVENANCE_ENTRY] ?: 0,
                canonicalDeviceMetadata = recordCounts[CANONICAL_DEVICE_METADATA_ENTRY] ?: 0,
                rawDeviceMetadata = recordCounts[RAW_DEVICE_METADATA_ENTRY] ?: 0
            )
        } catch (error: Exception) {
            directory.deleteRecursively()
            throw error
        }
    }

    /** Replace all rows in both stores in one SQLite transaction; settings and preferences remain. */
    fun restore(context: Context, plan: PreparedDatabaseRestore) {
        val events = File(plan.directory, EVENTS_ENTRY).bufferedReader(Charsets.UTF_8)
        val readings = File(plan.directory, READINGS_ENTRY).bufferedReader(Charsets.UTF_8)
        val cursors = File(plan.directory, CURSORS_ENTRY).bufferedReader(Charsets.UTF_8)
        val sourcesFile = File(plan.directory, FIT_SOURCES_ENTRY)
        val provenanceFile = File(plan.directory, FIT_PROVENANCE_ENTRY)
        events.use { eventReader -> readings.use { readingReader -> cursors.use { cursorReader ->
            CirqaDatabase(context).use { database ->
                if (plan.version == 1) {
                    database.replaceAllStoredRows(
                        eventReader.lineSequence(),
                        readingReader.lineSequence().map(::parseReading),
                        cursorReader.lineSequence().map(::parseCursor)
                    )
                } else if (plan.version == 2) {
                    sourcesFile.bufferedReader(Charsets.UTF_8).use { sourceReader ->
                        provenanceFile.bufferedReader(Charsets.UTF_8).use { provenanceReader ->
                            database.replaceAllStoredRows(
                                eventReader.lineSequence(),
                                readingReader.lineSequence().map(::parseReading),
                                cursorReader.lineSequence().map(::parseCursor),
                                sourceReader.lineSequence().map(::parseFitSource),
                                provenanceReader.lineSequence().map(::parseFitProvenance)
                            )
                        }
                    }
                } else {
                    sourcesFile.bufferedReader(Charsets.UTF_8).use { sourceReader ->
                        provenanceFile.bufferedReader(Charsets.UTF_8).use { provenanceReader ->
                            File(plan.directory, CANONICAL_READINGS_ENTRY).bufferedReader(Charsets.UTF_8).use { canonicalReadings ->
                                File(plan.directory, CANONICAL_CURSORS_ENTRY).bufferedReader(Charsets.UTF_8).use { canonicalCursors ->
                                    File(plan.directory, CANONICAL_FIT_SOURCES_ENTRY).bufferedReader(Charsets.UTF_8).use { canonicalSources ->
                                        File(plan.directory, CANONICAL_FIT_PROVENANCE_ENTRY).bufferedReader(Charsets.UTF_8).use { canonicalProvenance ->
                                                val replace: (
                                                Sequence<FitHistoricalObservations.DeviceMetadata>,
                                                Sequence<RawDeviceMetadataRecord>,
                                                Sequence<StoredFitUnmappedField>
                                            ) -> Unit = { metadata, rawMetadata, unmappedFields ->
                                                database.replaceAllStoredRows(
                                                    eventReader.lineSequence(),
                                                    readingReader.lineSequence().map(::parseReading),
                                                    cursorReader.lineSequence().map(::parseCursor),
                                                    sourceReader.lineSequence().map(::parseFitSource),
                                                    provenanceReader.lineSequence().map(::parseFitProvenance),
                                                    canonicalReadings.lineSequence().map(::parseReading),
                                                    canonicalCursors.lineSequence().map(::parseCursor),
                                                    canonicalSources.lineSequence().map(::parseFitSource),
                                                    canonicalProvenance.lineSequence().map(::parseFitProvenance),
                                                    canonicalDeviceMetadata = metadata,
                                                    canonicalRawDeviceMetadata = rawMetadata,
                                                    canonicalUnmappedFitFields = unmappedFields,
                                                    markHistoricalCopyComplete = true
                                                )
                                            }
                                            if (plan.version >= 4) {
                                                File(plan.directory, CANONICAL_DEVICE_METADATA_ENTRY)
                                                    .bufferedReader(Charsets.UTF_8).use { metadataReader ->
                                                        if (plan.version >= 5) {
                                                            File(plan.directory, RAW_DEVICE_METADATA_ENTRY)
                                                                .bufferedReader(Charsets.UTF_8).use { rawReader ->
                                                                    if (plan.version >= 6) {
                                                                        File(plan.directory, CANONICAL_UNMAPPED_FIT_FIELDS_ENTRY)
                                                                            .bufferedReader(Charsets.UTF_8).use { unknownReader ->
                                                                            database.replaceAllStoredRows(
                                                                                eventReader.lineSequence(),
                                                                                readingReader.lineSequence().map(::parseReading),
                                                                                cursorReader.lineSequence().map(::parseCursor),
                                                                                sourceReader.lineSequence().map(::parseFitSource),
                                                                                provenanceReader.lineSequence().map(::parseFitProvenance),
                                                                                canonicalReadings.lineSequence().map(::parseReading),
                                                                                canonicalCursors.lineSequence().map(::parseCursor),
                                                                                canonicalSources.lineSequence().map(::parseFitSource),
                                                                                canonicalProvenance.lineSequence().map(::parseFitProvenance),
                                                                                metadataReader.lineSequence().map(::parseDeviceMetadata),
                                                                                rawReader.lineSequence().map(::parseRawDeviceMetadata),
                                                                                unknownReader.lineSequence().map(::parseUnmappedFitField),
                                                                                markHistoricalCopyComplete = true
                                                                            )
                                                                        }
                                                                    } else replace(
                                                                        metadataReader.lineSequence().map(::parseDeviceMetadata),
                                                                        rawReader.lineSequence().map(::parseRawDeviceMetadata),
                                                                        emptySequence()
                                                                    )
                                                                }
                                                        } else {
                                                            replace(
                                                                metadataReader.lineSequence().map(::parseDeviceMetadata),
                                                                emptySequence(),
                                                                emptySequence()
                                                            )
                                                        }
                                                    }
                                            } else {
                                                replace(emptySequence(), emptySequence(), emptySequence())
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        } } }
    }

    /** Clear only the two local data stores. Profile, appearance and test settings are retained. */
    fun reset(context: Context) {
        CirqaDatabase(context).use { it.replaceAllStoredRows(emptySequence(), emptySequence(), emptySequence()) }
    }

    private fun writeArchive(context: Context, target: File): DatabaseBackupReport {
        val now = Instant.now().toString()
        val metadata = JSONObject()
        var eventCount = 0L
        var readingCount = 0L
        var cursorCount = 0L
        var fitSourceCount = 0L
        var fitProvenanceCount = 0L
        var canonicalReadingCount = 0L
        var canonicalCursorCount = 0L
        var canonicalFitSourceCount = 0L
        var canonicalFitProvenanceCount = 0L
        var canonicalDeviceMetadataCount = 0L
        var rawDeviceMetadataCount = 0L
        ZipOutputStream(BufferedOutputStream(FileOutputStream(target))).use { zip ->
            CirqaDatabase(context).use { database ->
                database.ensureHistoricalStoreCopied()
                database.readStoredSnapshot { db ->
                    val events = writeJsonLines(zip, EVENTS_ENTRY) { emit ->
                        db.rawQuery("SELECT payload_json FROM main.events ORDER BY received_at_utc, id", null).use { rows ->
                            while (rows.moveToNext()) emit(rows.getString(0))
                        }
                    }
                    eventCount = events.records
                    metadata.put(EVENTS_ENTRY, entryMetadata(events))

                    val readings = writeJsonLines(zip, READINGS_ENTRY) { emit ->
                        db.rawQuery(
                            """SELECT device_id, metric, measured_at_ms, value, unit, sample_id, received_at_ms
                               FROM legacy.readings ORDER BY measured_at_ms, device_id, metric, sample_id""", null
                        ).use { rows ->
                            while (rows.moveToNext()) emit(encodeReading(RawExportRecord(
                                deviceId = rows.getString(0), metric = rows.getString(1),
                                measuredAtEpochMillis = rows.getLong(2), value = rows.getDouble(3),
                                unit = rows.getString(4), sampleId = rows.getString(5).ifEmpty { null },
                                receivedAtEpochMillis = if (rows.isNull(6)) null else rows.getLong(6)
                            )))
                        }
                    }
                    readingCount = readings.records
                    metadata.put(READINGS_ENTRY, entryMetadata(readings))

                    val cursors = writeJsonLines(zip, CURSORS_ENTRY) { emit ->
                        db.rawQuery(
                            "SELECT device_id, last_committed_ms FROM legacy.sync_cursors ORDER BY device_id", null
                        ).use { rows ->
                            while (rows.moveToNext()) emit(encodeCursor(
                                LocalSyncCursor(rows.getString(0), rows.getLong(1))
                            ))
                        }
                    }
                    cursorCount = cursors.records
                    metadata.put(CURSORS_ENTRY, entryMetadata(cursors))

                    val sources = writeJsonLines(zip, FIT_SOURCES_ENTRY) { emit ->
                        db.rawQuery(
                            """SELECT source_sha256, file_bytes, relative_path, validated_at_ms,
                                      decoder_version, profile_version
                               FROM legacy.fit_sources ORDER BY source_sha256""", null
                        ).use { rows ->
                            while (rows.moveToNext()) emit(encodeFitSource(StoredFitSource(
                                sha256 = rows.getString(0), fileBytes = rows.getLong(1),
                                relativePath = rows.getString(2), validatedAtEpochMillis = rows.getLong(3),
                                decoderVersion = rows.getString(4), profileVersion = rows.getString(5)
                            )))
                        }
                    }
                    fitSourceCount = sources.records
                    metadata.put(FIT_SOURCES_ENTRY, entryMetadata(sources))

                    val provenance = writeJsonLines(zip, FIT_PROVENANCE_ENTRY) { emit ->
                        db.rawQuery(
                            """SELECT sample_id, device_id, metric, measured_at_ms, source_sha256,
                                      record_ordinal, global_message_number, field_number, quality, decoder_version
                               FROM legacy.fit_observation_provenance
                               ORDER BY source_sha256, record_ordinal, global_message_number, field_number""", null
                        ).use { rows ->
                            while (rows.moveToNext()) emit(encodeFitProvenance(StoredFitProvenance(
                                sampleId = rows.getString(0), deviceId = rows.getString(1), metric = rows.getString(2),
                                measuredAtEpochMillis = rows.getLong(3), sourceSha256 = rows.getString(4),
                                recordOrdinal = rows.getInt(5), globalMessageNumber = rows.getInt(6),
                                fieldNumber = rows.getInt(7), quality = rows.getString(8),
                                decoderVersion = rows.getString(9)
                            )))
                        }
                    }
                    fitProvenanceCount = provenance.records
                    metadata.put(FIT_PROVENANCE_ENTRY, entryMetadata(provenance))

                    val canonicalReadings = writeJsonLines(zip, CANONICAL_READINGS_ENTRY) { emit ->
                        db.rawQuery(
                            """SELECT device_id, metric, measured_at_ms, value, unit, sample_id, received_at_ms
                               FROM main.observations ORDER BY measured_at_ms, device_id, metric, sample_id""", null
                        ).use { rows ->
                            while (rows.moveToNext()) emit(encodeReading(RawExportRecord(
                                deviceId = rows.getString(0), metric = rows.getString(1),
                                measuredAtEpochMillis = rows.getLong(2), value = rows.getDouble(3),
                                unit = rows.getString(4), sampleId = rows.getString(5).ifEmpty { null },
                                receivedAtEpochMillis = if (rows.isNull(6)) null else rows.getLong(6)
                            )))
                        }
                    }
                    canonicalReadingCount = canonicalReadings.records
                    metadata.put(CANONICAL_READINGS_ENTRY, entryMetadata(canonicalReadings))

                    val canonicalCursors = writeJsonLines(zip, CANONICAL_CURSORS_ENTRY) { emit ->
                        db.rawQuery(
                            "SELECT device_id, last_committed_ms FROM main.sync_cursors ORDER BY device_id", null
                        ).use { rows ->
                            while (rows.moveToNext()) emit(encodeCursor(
                                LocalSyncCursor(rows.getString(0), rows.getLong(1))
                            ))
                        }
                    }
                    canonicalCursorCount = canonicalCursors.records
                    metadata.put(CANONICAL_CURSORS_ENTRY, entryMetadata(canonicalCursors))

                    val canonicalSources = writeJsonLines(zip, CANONICAL_FIT_SOURCES_ENTRY) { emit ->
                        db.rawQuery(
                            """SELECT source_sha256, file_bytes, relative_path, validated_at_ms,
                                      decoder_version, profile_version
                               FROM main.fit_sources ORDER BY source_sha256""", null
                        ).use { rows ->
                            while (rows.moveToNext()) emit(encodeFitSource(StoredFitSource(
                                sha256 = rows.getString(0), fileBytes = rows.getLong(1),
                                relativePath = rows.getString(2), validatedAtEpochMillis = rows.getLong(3),
                                decoderVersion = rows.getString(4), profileVersion = rows.getString(5)
                            )))
                        }
                    }
                    canonicalFitSourceCount = canonicalSources.records
                    metadata.put(CANONICAL_FIT_SOURCES_ENTRY, entryMetadata(canonicalSources))

                    val canonicalProvenance = writeJsonLines(zip, CANONICAL_FIT_PROVENANCE_ENTRY) { emit ->
                        db.rawQuery(
                            """SELECT sample_id, device_id, metric, measured_at_ms, source_sha256,
                                      record_ordinal, global_message_number, field_number, quality, decoder_version
                               FROM main.fit_observation_provenance
                               ORDER BY source_sha256, record_ordinal, global_message_number, field_number""", null
                        ).use { rows ->
                            while (rows.moveToNext()) emit(encodeFitProvenance(StoredFitProvenance(
                                sampleId = rows.getString(0), deviceId = rows.getString(1), metric = rows.getString(2),
                                measuredAtEpochMillis = rows.getLong(3), sourceSha256 = rows.getString(4),
                                recordOrdinal = rows.getInt(5), globalMessageNumber = rows.getInt(6),
                                fieldNumber = rows.getInt(7), quality = rows.getString(8),
                                decoderVersion = rows.getString(9)
                            )))
                        }
                    }
                    canonicalFitProvenanceCount = canonicalProvenance.records
                    metadata.put(CANONICAL_FIT_PROVENANCE_ENTRY, entryMetadata(canonicalProvenance))

                    val canonicalDeviceMetadata = writeJsonLines(zip, CANONICAL_DEVICE_METADATA_ENTRY) { emit ->
                        db.rawQuery(
                            """SELECT source_sha256, record_ordinal, observed_at_ms, device_type_code,
                                      manufacturer_code, product_code, software_version, battery_voltage_volts,
                                      battery_status_code, battery_level_percent
                               FROM main.fit_device_metadata
                               ORDER BY observed_at_ms, source_sha256, record_ordinal""", null
                        ).use { rows ->
                            while (rows.moveToNext()) emit(encodeDeviceMetadata(FitHistoricalObservations.DeviceMetadata(
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
                            )))
                        }
                    }
                    canonicalDeviceMetadataCount = canonicalDeviceMetadata.records
                    metadata.put(CANONICAL_DEVICE_METADATA_ENTRY, entryMetadata(canonicalDeviceMetadata))
                    val rawDeviceMetadata = writeJsonLines(zip, RAW_DEVICE_METADATA_ENTRY) { emit ->
                        db.rawQuery(
                            """SELECT source_sha256, file_bytes, verified_at_ms, parser_version,
                                      software_version, mass_storage_mode, transfer_direction_count
                               FROM main.raw_device_metadata ORDER BY verified_at_ms, source_sha256""",
                            null
                        ).use { rows ->
                            while (rows.moveToNext()) emit(encodeRawDeviceMetadata(RawDeviceMetadataRecord(
                                sourceSha256 = rows.getString(0),
                                fileBytes = rows.getLong(1),
                                verifiedAtEpochMillis = rows.getLong(2),
                                parserVersion = rows.getString(3),
                                softwareVersion = if (rows.isNull(4)) null else rows.getString(4),
                                massStorageMode = if (rows.isNull(5)) null else rows.getInt(5) == 1,
                                transferDirectionElementCount = rows.getInt(6)
                            )))
                        }
                    }
                    rawDeviceMetadataCount = rawDeviceMetadata.records
                    metadata.put(RAW_DEVICE_METADATA_ENTRY, entryMetadata(rawDeviceMetadata))
                    val canonicalUnmappedFields = writeJsonLines(zip, CANONICAL_UNMAPPED_FIT_FIELDS_ENTRY) { emit ->
                        db.rawQuery(
                            """SELECT source_sha256, global_message_number, field_kind, field_number,
                                      base_type_code, field_size, developer_data_index, classification,
                                      definition_count, observed_message_count, valid_element_count,
                                      invalid_element_count, decoder_version, profile_version, review_status
                               FROM main.fit_unmapped_fields
                               ORDER BY source_sha256, global_message_number, field_kind, field_number""",
                            null
                        ).use { rows ->
                            while (rows.moveToNext()) emit(encodeUnmappedFitField(StoredFitUnmappedField(
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
                            )))
                        }
                    }
                    metadata.put(CANONICAL_UNMAPPED_FIT_FIELDS_ENTRY, entryMetadata(canonicalUnmappedFields))
                    require(
                        events.bytes + readings.bytes + cursors.bytes + sources.bytes + provenance.bytes +
                            canonicalReadings.bytes + canonicalCursors.bytes + canonicalSources.bytes +
                            canonicalProvenance.bytes + canonicalDeviceMetadata.bytes +
                            rawDeviceMetadata.bytes + canonicalUnmappedFields.bytes <= MAX_ARCHIVE_BYTES
                    ) {
                        "Lokale Datenbank ist zu groß für ein Backup"
                    }
                }
            }
            val manifest = JSONObject()
                .put("format", BACKUP_FORMAT)
                .put("version", BACKUP_VERSION)
                .put("app_id", "de.pulselibre")
                .put("created_at", now)
                .put("encryption", "none")
                .put("entries", metadata)
            zip.putNextEntry(ZipEntry(MANIFEST_ENTRY))
            zip.write(manifest.toString().toByteArray(Charsets.UTF_8))
            zip.closeEntry()
        }
        require(target.length() <= MAX_ARCHIVE_BYTES) { "Backup-Datei überschreitet das Größenlimit" }
        return DatabaseBackupReport(
            now, eventCount, readingCount, cursorCount, fitSourceCount, fitProvenanceCount,
            canonicalReadingCount, canonicalCursorCount, canonicalFitSourceCount,
            canonicalFitProvenanceCount, canonicalDeviceMetadataCount, rawDeviceMetadataCount
        )
    }

    private data class JsonLinesMetadata(val records: Long, val bytes: Long, val sha256: String)

    private fun writeJsonLines(zip: ZipOutputStream, name: String, produce: ((String) -> Unit) -> Unit): JsonLinesMetadata {
        zip.putNextEntry(ZipEntry(name))
        val digest = MessageDigest.getInstance("SHA-256")
        val writer = BufferedWriter(OutputStreamWriter(DigestOutputStream(zip, digest), Charsets.UTF_8))
        var records = 0L
        var bytes = 0L
        produce { line ->
            require(line.isNotBlank() && '\n' !in line && '\r' !in line) { "Ungültige JSONL-Daten" }
            val lineBytes = line.toByteArray(Charsets.UTF_8).size
            require(lineBytes <= MAX_LINE_BYTES) { "Ein Datensatz überschreitet das Größenlimit" }
            records++
            bytes += lineBytes + 1
            require(records <= MAX_RECORDS && bytes <= MAX_ENTRY_BYTES) { "Datenbankbackup überschreitet das Größenlimit" }
            writer.write(line)
            writer.newLine()
        }
        writer.flush()
        zip.closeEntry()
        return JsonLinesMetadata(records, bytes, digest.digest().toHex())
    }

    private fun entryMetadata(value: JsonLinesMetadata) = JSONObject()
        .put("records", value.records)
        .put("bytes", value.bytes)
        .put("sha256", value.sha256)

    private fun extractArchive(source: InputStream, directory: File): Map<String, File> {
        val allowed = BACKUP_V5_ENTRIES + MANIFEST_ENTRY
        val files = linkedMapOf<String, File>()
        var totalBytes = 0L
        ZipInputStream(BufferedInputStream(source)).use { zip ->
            while (true) {
                val entry = zip.nextEntry ?: break
                require(!entry.isDirectory && entry.name in allowed) { "Backup enthält einen unbekannten Dateieintrag" }
                require(entry.name !in files) { "Backup enthält doppelte Dateieinträge" }
                val target = File(directory, entry.name)
                var entryBytes = 0L
                FileOutputStream(target).use { out ->
                    val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                    while (true) {
                        val count = zip.read(buffer)
                        if (count < 0) break
                        entryBytes += count
                        totalBytes += count
                        require(entryBytes <= MAX_ENTRY_BYTES && totalBytes <= MAX_ARCHIVE_BYTES) {
                            "Backup ist zu groß"
                        }
                        out.write(buffer, 0, count)
                    }
                }
                zip.closeEntry()
                files[entry.name] = target
            }
        }
        require(MANIFEST_ENTRY in files) { "Backup-Manifest fehlt" }
        require(files.getValue(MANIFEST_ENTRY).length() <= MAX_MANIFEST_BYTES) { "Backup-Manifest ist zu groß" }
        return files
    }

    private fun validateJsonLines(name: String, file: File): Long {
        var count = 0L
        file.bufferedReader(Charsets.UTF_8).useLines { lines ->
            lines.forEach { line ->
                require(line.isNotBlank() && line.toByteArray(Charsets.UTF_8).size <= MAX_LINE_BYTES) {
                    "Ungültige oder zu große Zeile in $name"
                }
                when (name) {
                    EVENTS_ENTRY -> require(CirqaDatabase.isValidStoredLine(line)) { "Ungültiger Ereigniseintrag im Backup" }
                    READINGS_ENTRY -> parseReading(line)
                    CURSORS_ENTRY -> parseCursor(line)
                    FIT_SOURCES_ENTRY -> parseFitSource(line)
                    FIT_PROVENANCE_ENTRY -> parseFitProvenance(line)
                    CANONICAL_READINGS_ENTRY -> parseReading(line)
                    CANONICAL_CURSORS_ENTRY -> parseCursor(line)
                    CANONICAL_FIT_SOURCES_ENTRY -> parseFitSource(line)
                    CANONICAL_FIT_PROVENANCE_ENTRY -> parseFitProvenance(line)
                    CANONICAL_DEVICE_METADATA_ENTRY -> parseDeviceMetadata(line)
                    RAW_DEVICE_METADATA_ENTRY -> parseRawDeviceMetadata(line)
                    CANONICAL_UNMAPPED_FIT_FIELDS_ENTRY -> parseUnmappedFitField(line)
                }
                count++
                require(count <= MAX_RECORDS) { "Zu viele Einträge in $name" }
            }
        }
        return count
    }

    /** Validate source/reading foreign-key relationships before restore confirmation. */
    private fun validateFitReferences(
        directory: File,
        readingsEntry: String,
        sourcesEntry: String,
        provenanceEntry: String
    ) {
        val stagingFile = File(directory, ".fit-reference-validation.db")
        val db = SQLiteDatabase.openOrCreateDatabase(stagingFile, null)
        try {
            db.setForeignKeyConstraintsEnabled(true)
            db.execSQL(
                """CREATE TABLE sources (
                   source_sha256 TEXT PRIMARY KEY NOT NULL
                )"""
            )
            db.execSQL(
                """CREATE TABLE readings (
                   device_id TEXT NOT NULL, metric TEXT NOT NULL, measured_at_ms INTEGER NOT NULL,
                   sample_id TEXT NOT NULL, PRIMARY KEY(device_id, metric, measured_at_ms, sample_id)
                )"""
            )
            db.execSQL(
                """CREATE TABLE provenance (
                   sample_id TEXT PRIMARY KEY NOT NULL, device_id TEXT NOT NULL, metric TEXT NOT NULL,
                   measured_at_ms INTEGER NOT NULL, source_sha256 TEXT NOT NULL,
                   FOREIGN KEY(source_sha256) REFERENCES sources(source_sha256),
                   FOREIGN KEY(device_id, metric, measured_at_ms, sample_id)
                       REFERENCES readings(device_id, metric, measured_at_ms, sample_id)
                )"""
            )
            db.beginTransaction()
            try {
                File(directory, sourcesEntry).bufferedReader(Charsets.UTF_8).useLines { lines ->
                    lines.forEach { line ->
                        val source = parseFitSource(line)
                        db.execSQL("INSERT INTO sources(source_sha256) VALUES(?)", arrayOf(source.sha256))
                    }
                }
                File(directory, readingsEntry).bufferedReader(Charsets.UTF_8).useLines { lines ->
                    lines.forEach { line ->
                        val reading = parseReading(line)
                        db.execSQL(
                            """INSERT INTO readings(device_id, metric, measured_at_ms, sample_id)
                               VALUES(?, ?, ?, ?)""",
                            arrayOf<Any?>(reading.deviceId, reading.metric, reading.measuredAtEpochMillis,
                                reading.sampleId ?: "")
                        )
                    }
                }
                File(directory, provenanceEntry).bufferedReader(Charsets.UTF_8).useLines { lines ->
                    lines.forEach { line ->
                        val item = parseFitProvenance(line)
                        db.execSQL(
                            """INSERT INTO provenance
                               (sample_id, device_id, metric, measured_at_ms, source_sha256)
                               VALUES(?, ?, ?, ?, ?)""",
                            arrayOf<Any?>(item.sampleId, item.deviceId, item.metric, item.measuredAtEpochMillis,
                                item.sourceSha256)
                        )
                    }
                }
                db.setTransactionSuccessful()
            } finally {
                db.endTransaction()
            }
        } finally {
            db.close()
            stagingFile.delete()
            File(stagingFile.path + "-journal").delete()
        }
    }

    private fun validateDeviceMetadataReferences(directory: File) {
        val stagingFile = File(directory, ".device-metadata-reference-validation.db")
        val db = SQLiteDatabase.openOrCreateDatabase(stagingFile, null)
        try {
            db.setForeignKeyConstraintsEnabled(true)
            db.execSQL("CREATE TABLE sources(source_sha256 TEXT PRIMARY KEY NOT NULL)")
            db.execSQL(
                """CREATE TABLE metadata(
                   source_sha256 TEXT NOT NULL REFERENCES sources(source_sha256),
                   record_ordinal INTEGER NOT NULL,
                   PRIMARY KEY(source_sha256, record_ordinal)
                )"""
            )
            db.beginTransaction()
            try {
                File(directory, CANONICAL_FIT_SOURCES_ENTRY).bufferedReader(Charsets.UTF_8).useLines { lines ->
                    lines.forEach { line ->
                        db.execSQL("INSERT INTO sources(source_sha256) VALUES(?)", arrayOf(parseFitSource(line).sha256))
                    }
                }
                File(directory, CANONICAL_DEVICE_METADATA_ENTRY).bufferedReader(Charsets.UTF_8).useLines { lines ->
                    lines.forEach { line ->
                        val item = parseDeviceMetadata(line)
                        db.execSQL(
                            "INSERT INTO metadata(source_sha256, record_ordinal) VALUES(?, ?)",
                            arrayOf<Any?>(item.sourceFileSha256, item.recordOrdinal)
                        )
                    }
                }
                db.rawQuery("PRAGMA foreign_key_check", null).use {
                    require(!it.moveToFirst()) { "FIT-Gerätemetadaten verweisen auf eine fehlende Quelle" }
                }
                db.setTransactionSuccessful()
            } finally {
                db.endTransaction()
            }
        } finally {
            db.close()
            stagingFile.delete()
            File(stagingFile.path + "-journal").delete()
        }
    }

    private fun validateRawDeviceMetadataReferences(directory: File) {
        val stagingFile = File(directory, ".raw-device-metadata-validation.db")
        val db = SQLiteDatabase.openOrCreateDatabase(stagingFile, null)
        try {
            db.execSQL("CREATE TABLE metadata(source_sha256 TEXT PRIMARY KEY NOT NULL)")
            db.beginTransaction()
            try {
                File(directory, RAW_DEVICE_METADATA_ENTRY).bufferedReader(Charsets.UTF_8).useLines { lines ->
                    lines.forEach { line ->
                        val item = parseRawDeviceMetadata(line)
                        db.execSQL("INSERT INTO metadata(source_sha256) VALUES(?)", arrayOf(item.sourceSha256))
                    }
                }
                db.setTransactionSuccessful()
            } finally {
                db.endTransaction()
            }
        } finally {
            db.close()
            stagingFile.delete()
            File(stagingFile.path + "-journal").delete()
        }
    }

    private fun validateUnmappedFitFieldReferences(directory: File) {
        val sources = hashSetOf<String>()
        File(directory, CANONICAL_FIT_SOURCES_ENTRY).bufferedReader(Charsets.UTF_8).useLines { lines ->
            lines.forEach { sources += parseFitSource(it).sha256 }
        }
        File(directory, CANONICAL_UNMAPPED_FIT_FIELDS_ENTRY).bufferedReader(Charsets.UTF_8).useLines { lines ->
            lines.forEach { line ->
                val field = parseUnmappedFitField(line)
                require(field.sourceSha256 in sources) { "FIT-Feldinventur verweist auf eine fehlende Quelle" }
            }
        }
    }

    private fun parseReading(line: String): RawExportRecord {
        val item = JSONObject(line)
        val record = RawExportRecord(
            deviceId = item.getString("device_id"), metric = item.getString("metric"),
            measuredAtEpochMillis = item.getLong("measured_at_ms"), value = item.getDouble("value"),
            unit = item.getString("unit"), sampleId = item.optString("sample_id").ifEmpty { null },
            receivedAtEpochMillis = if (!item.has("received_at_ms") || item.isNull("received_at_ms")) null
                else item.getLong("received_at_ms")
        )
        require(CirqaDatabase.isValidStoredReading(record)) { "Ungültiger Messwert im Backup" }
        return record
    }

    private fun parseCursor(line: String): LocalSyncCursor {
        val item = JSONObject(line)
        val cursor = LocalSyncCursor(item.getString("device_id"), item.getLong("last_committed_ms"))
        require(cursor.deviceId.isNotBlank() && cursor.lastCommittedEpochMillis >= 0) {
            "Ungültiger Synchronisationsstand im Backup"
        }
        return cursor
    }

    private fun parseFitSource(line: String): StoredFitSource {
        val item = JSONObject(line)
        val source = StoredFitSource(
            sha256 = item.getString("sha256"),
            fileBytes = item.getLong("file_bytes"),
            relativePath = item.getString("relative_path"),
            validatedAtEpochMillis = item.getLong("validated_at_ms"),
            decoderVersion = item.getString("decoder_version"),
            profileVersion = item.getString("profile_version")
        )
        require(source.sha256.matches(Regex("[0-9a-f]{64}")) && source.fileBytes > 0 &&
            source.validatedAtEpochMillis >= 0 &&
            source.relativePath == "verified_fit/${source.sha256}.fit" &&
            source.decoderVersion.isNotBlank() && source.profileVersion.isNotBlank()) {
            "Ungültige FIT-Quellmetadaten im Backup"
        }
        return source
    }

    private fun parseFitProvenance(line: String): StoredFitProvenance {
        val item = JSONObject(line)
        val provenance = StoredFitProvenance(
            sampleId = item.getString("sample_id"),
            deviceId = item.getString("device_id"),
            metric = item.getString("metric"),
            measuredAtEpochMillis = item.getLong("measured_at_ms"),
            sourceSha256 = item.getString("source_sha256"),
            recordOrdinal = item.getInt("record_ordinal"),
            globalMessageNumber = item.getInt("global_message_number"),
            fieldNumber = item.getInt("field_number"),
            quality = item.getString("quality"),
            decoderVersion = item.getString("decoder_version")
        )
        require(provenance.sampleId.isNotBlank() && provenance.deviceId.isNotBlank() &&
            provenance.metric.isNotBlank() && provenance.measuredAtEpochMillis >= 0 &&
            provenance.sourceSha256.matches(Regex("[0-9a-f]{64}")) &&
            provenance.recordOrdinal >= 0 && provenance.globalMessageNumber >= 0 &&
            provenance.fieldNumber >= 0 && provenance.quality == "PROFILE_VALID" &&
            provenance.decoderVersion.isNotBlank()) {
            "Ungültige FIT-Feldherkunft im Backup"
        }
        return provenance
    }

    private fun parseDeviceMetadata(line: String): FitHistoricalObservations.DeviceMetadata {
        val item = JSONObject(line)
        val metadata = FitHistoricalObservations.DeviceMetadata(
            sourceFileSha256 = item.getString("source_sha256"),
            recordOrdinal = item.getInt("record_ordinal"),
            timestampEpochMillis = if (!item.has("observed_at_ms") || item.isNull("observed_at_ms"))
                null else item.getLong("observed_at_ms"),
            deviceTypeCode = if (!item.has("device_type_code") || item.isNull("device_type_code"))
                null else item.getInt("device_type_code"),
            manufacturerCode = if (!item.has("manufacturer_code") || item.isNull("manufacturer_code"))
                null else item.getInt("manufacturer_code"),
            productCode = if (!item.has("product_code") || item.isNull("product_code"))
                null else item.getInt("product_code"),
            softwareVersion = if (!item.has("software_version") || item.isNull("software_version"))
                null else item.getDouble("software_version"),
            batteryVoltageVolts = if (!item.has("battery_voltage_volts") || item.isNull("battery_voltage_volts"))
                null else item.getDouble("battery_voltage_volts"),
            batteryStatusCode = if (!item.has("battery_status_code") || item.isNull("battery_status_code"))
                null else item.getInt("battery_status_code"),
            batteryLevelPercent = if (!item.has("battery_level_percent") || item.isNull("battery_level_percent"))
                null else item.getInt("battery_level_percent")
        )
        require(CirqaDatabase.isValidDeviceMetadata(metadata)) { "Ungültige FIT-Gerätemetadaten im Backup" }
        return metadata
    }

    private fun parseRawDeviceMetadata(line: String): RawDeviceMetadataRecord {
        val item = JSONObject(line)
        val metadata = RawDeviceMetadataRecord(
            sourceSha256 = item.getString("source_sha256"),
            fileBytes = item.getLong("file_bytes"),
            verifiedAtEpochMillis = item.getLong("verified_at_ms"),
            parserVersion = item.getString("parser_version"),
            softwareVersion = if (!item.has("software_version") || item.isNull("software_version"))
                null else item.getString("software_version"),
            massStorageMode = if (!item.has("mass_storage_mode") || item.isNull("mass_storage_mode"))
                null else item.getBoolean("mass_storage_mode"),
            transferDirectionElementCount = item.getInt("transfer_direction_count")
        )
        require(CirqaDatabase.isValidRawDeviceMetadata(metadata)) {
            "Ungültige Raw-Gerätemetadaten im Backup"
        }
        return metadata
    }

    private fun parseUnmappedFitField(line: String): StoredFitUnmappedField {
        val json = JSONObject(line)
        val item = StoredFitUnmappedField(
            sourceSha256 = json.getString("source_sha256"),
            globalMessageNumber = json.getInt("global_message_number"),
            kind = StoredFitFieldKind.valueOf(json.getString("field_kind")),
            fieldNumber = json.getInt("field_number"),
            baseTypeCode = if (json.isNull("base_type_code")) null else json.getInt("base_type_code"),
            fieldSize = json.getInt("field_size"),
            developerDataIndex = if (json.isNull("developer_data_index")) null else json.getInt("developer_data_index"),
            classification = json.getString("classification"),
            definitionCount = json.getLong("definition_count"),
            observedMessageCount = json.getLong("observed_message_count"),
            validElementCount = if (json.isNull("valid_element_count")) null else json.getLong("valid_element_count"),
            invalidElementCount = if (json.isNull("invalid_element_count")) null else json.getLong("invalid_element_count"),
            decoderVersion = json.getString("decoder_version"),
            profileVersion = json.getString("profile_version"),
            status = FitFieldInventoryStatus.valueOf(json.getString("status"))
        )
        require(
            item.sourceSha256.matches(Regex("[0-9a-f]{64}")) &&
                item.globalMessageNumber in 0..65535 && item.fieldNumber in 0..255 &&
                item.fieldSize in 1..255 &&
                (item.baseTypeCode == null || item.baseTypeCode in 0..255) &&
                (item.developerDataIndex == null || item.developerDataIndex in 0..255) &&
                item.classification in setOf(
                    "MESSAGE_NOT_DECODED_BY_APP", "FIELD_NOT_DECODED_BY_APP",
                    "KNOWN_MAPPING_INCOMPATIBLE", "DEVELOPER_FIELD_NOT_DECODED_BY_APP"
                ) && item.definitionCount in 0..1_000_000 &&
                item.observedMessageCount in 0..1_000_000 &&
                (item.validElementCount == null || item.validElementCount in 0..1_000_000) &&
                (item.invalidElementCount == null || item.invalidElementCount in 0..1_000_000) &&
                item.decoderVersion.length in 1..32 && item.profileVersion.length in 1..32
        ) { "Ungültige FIT-Feldinventur im Backup" }
        return item
    }

    private fun encodeReading(record: RawExportRecord): String = JSONObject()
        .put("device_id", record.deviceId)
        .put("metric", record.metric)
        .put("measured_at_ms", record.measuredAtEpochMillis)
        .put("value", record.value)
        .put("unit", record.unit)
        .put("sample_id", record.sampleId ?: "")
        .put("received_at_ms", record.receivedAtEpochMillis ?: JSONObject.NULL)
        .toString()

    private fun encodeCursor(cursor: LocalSyncCursor): String = JSONObject()
        .put("device_id", cursor.deviceId)
        .put("last_committed_ms", cursor.lastCommittedEpochMillis)
        .toString()

    private fun encodeFitSource(source: StoredFitSource): String = JSONObject()
        .put("sha256", source.sha256)
        .put("file_bytes", source.fileBytes)
        .put("relative_path", source.relativePath)
        .put("validated_at_ms", source.validatedAtEpochMillis)
        .put("decoder_version", source.decoderVersion)
        .put("profile_version", source.profileVersion)
        .toString()

    private fun encodeFitProvenance(value: StoredFitProvenance): String = JSONObject()
        .put("sample_id", value.sampleId)
        .put("device_id", value.deviceId)
        .put("metric", value.metric)
        .put("measured_at_ms", value.measuredAtEpochMillis)
        .put("source_sha256", value.sourceSha256)
        .put("record_ordinal", value.recordOrdinal)
        .put("global_message_number", value.globalMessageNumber)
        .put("field_number", value.fieldNumber)
        .put("quality", value.quality)
        .put("decoder_version", value.decoderVersion)
        .toString()

    private fun encodeDeviceMetadata(value: FitHistoricalObservations.DeviceMetadata): String = JSONObject()
        .put("source_sha256", value.sourceFileSha256)
        .put("record_ordinal", value.recordOrdinal)
        .put("observed_at_ms", value.timestampEpochMillis ?: JSONObject.NULL)
        .put("device_type_code", value.deviceTypeCode ?: JSONObject.NULL)
        .put("manufacturer_code", value.manufacturerCode ?: JSONObject.NULL)
        .put("product_code", value.productCode ?: JSONObject.NULL)
        .put("software_version", value.softwareVersion ?: JSONObject.NULL)
        .put("battery_voltage_volts", value.batteryVoltageVolts ?: JSONObject.NULL)
        .put("battery_status_code", value.batteryStatusCode ?: JSONObject.NULL)
        .put("battery_level_percent", value.batteryLevelPercent ?: JSONObject.NULL)
        .toString()

    private fun encodeRawDeviceMetadata(value: RawDeviceMetadataRecord): String = JSONObject()
        .put("source_sha256", value.sourceSha256)
        .put("file_bytes", value.fileBytes)
        .put("verified_at_ms", value.verifiedAtEpochMillis)
        .put("parser_version", value.parserVersion)
        .put("software_version", value.softwareVersion ?: JSONObject.NULL)
        .put("mass_storage_mode", value.massStorageMode ?: JSONObject.NULL)
        .put("transfer_direction_count", value.transferDirectionElementCount)
        .toString()

    private fun encodeUnmappedFitField(value: StoredFitUnmappedField): String = JSONObject()
        .put("source_sha256", value.sourceSha256)
        .put("global_message_number", value.globalMessageNumber)
        .put("field_kind", value.kind.name)
        .put("field_number", value.fieldNumber)
        .put("base_type_code", value.baseTypeCode ?: JSONObject.NULL)
        .put("field_size", value.fieldSize)
        .put("developer_data_index", value.developerDataIndex ?: JSONObject.NULL)
        .put("classification", value.classification)
        .put("definition_count", value.definitionCount)
        .put("observed_message_count", value.observedMessageCount)
        .put("valid_element_count", value.validElementCount ?: JSONObject.NULL)
        .put("invalid_element_count", value.invalidElementCount ?: JSONObject.NULL)
        .put("decoder_version", value.decoderVersion)
        .put("profile_version", value.profileVersion)
        .put("status", value.status.name)
        .toString()

    private fun sha256(file: File): String {
        val digest = MessageDigest.getInstance("SHA-256")
        DigestInputStream(BufferedInputStream(FileInputStream(file)), digest).use { input ->
            val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
            while (input.read(buffer) >= 0) Unit
        }
        return digest.digest().toHex()
    }

    private fun ByteArray.toHex(): String = joinToString("") { "%02x".format(it.toInt() and 255) }
}
