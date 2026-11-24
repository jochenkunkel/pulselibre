package de.pulselibre

import android.content.Context
import android.content.ContextWrapper
import android.database.DatabaseErrorHandler
import android.database.sqlite.SQLiteDatabase
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.nio.file.Files
import java.security.MessageDigest
import java.time.Instant
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import org.json.JSONObject

@RunWith(AndroidJUnit4::class)
class DatabaseBackupTest {
    @Test fun schemaSixUpgradeCreatesImmutableUnknownTimestampGenerationWithoutChangingLegacyRows() {
        val base = ApplicationProvider.getApplicationContext<Context>()
        val root = Files.createTempDirectory(base.cacheDir.toPath(), "fit-generation-migration-").toFile()
        val context = isolatedContext(base, root)
        val sourceSha = "f".repeat(64)
        val observation = FitHistoricalObservations.Observation(
            metric = FitHistoricalObservations.Metric.HEART_RATE,
            timestampEpochMillis = 123_456,
            value = 71.0,
            unit = "bpm",
            sourceFileSha256 = sourceSha,
            recordOrdinal = 7,
            globalMessageNumber = 20,
            fieldNumber = 3,
            quality = FitHistoricalObservations.Quality.PROFILE_VALID,
            idempotencyKey = "generation-migration-sample",
            duplicateFingerprint = "migration-fingerprint"
        )
        try {
            CirqaDatabase(context).use { database ->
                database.commitFitFile(
                    sourceSha256 = sourceSha,
                    fileBytes = 100,
                    relativePath = "verified_fit/$sourceSha.fit",
                    validatedAtEpochMillis = 987,
                    decoderVersion = FitHistoricalObservations.DECODER_VERSION,
                    profileVersion = FitHistoricalObservations.PROFILE_VERSION,
                    observations = listOf(observation)
                )
                val db = database.writableDatabase
                db.execSQL("DROP TABLE fit_active_generations")
                db.execSQL("DROP TABLE fit_generation_observations")
                db.execSQL("DROP TABLE fit_decoder_generations")
                db.version = 6
            }

            CirqaDatabase(context).use { database ->
                val db = database.readableDatabase
                assertEquals(11, db.version)
                db.rawQuery(
                    """SELECT g.source_sha256, g.decoder_version, g.profile_version,
                              g.output_count, a.generation_id
                       FROM fit_decoder_generations g
                       JOIN fit_active_generations a ON a.generation_id = g.generation_id
                       WHERE g.source_sha256 = ?""",
                    arrayOf(sourceSha)
                ).use { rows ->
                    assertTrue(rows.moveToFirst())
                    assertEquals(sourceSha, rows.getString(0))
                    assertEquals("legacy-unknown-v1", rows.getString(1))
                    assertEquals(FitHistoricalObservations.PROFILE_VERSION, rows.getString(2))
                    assertEquals(1, rows.getInt(3))
                    assertEquals(64, rows.getString(4).length)
                    assertFalse(rows.moveToNext())
                }
                db.rawQuery(
                    """SELECT sample_id, value, record_ordinal, timestamp_origin
                       FROM fit_generation_observations WHERE generation_id =
                         (SELECT generation_id FROM fit_active_generations WHERE source_sha256 = ?)""",
                    arrayOf(sourceSha)
                ).use { rows ->
                    assertTrue(rows.moveToFirst())
                    assertEquals("generation-migration-sample", rows.getString(0))
                    assertEquals(71.0, rows.getDouble(1), 0.0)
                    assertEquals(7, rows.getInt(2))
                    assertEquals("UNKNOWN", rows.getString(3))
                    assertFalse(rows.moveToNext())
                }
                db.rawQuery(
                    "SELECT value FROM observations WHERE sample_id = ?",
                    arrayOf("generation-migration-sample")
                ).use { rows ->
                    assertTrue("Original row remains unchanged", rows.moveToFirst())
                    assertEquals(71.0, rows.getDouble(0), 0.0)
                }
            }
            val activePage = HistoricalObservationRepository(context).read(
                HistoricalObservationQuery(
                    startInclusive = Instant.EPOCH,
                    endExclusive = Instant.ofEpochMilli(Long.MAX_VALUE),
                    limit = 20
                )
            )
            assertEquals(1, activePage.observations.size)
            assertEquals("UNKNOWN", activePage.observations.single().provenance?.timestampOrigin)

            val archive = ByteArrayOutputStream()
            DatabaseBackup.write(context, archive)
            val plan = DatabaseBackup.prepareRestore(context, ByteArrayInputStream(archive.toByteArray()))
            plan.use {
                assertEquals(1L, it.canonicalFitGenerations)
                assertEquals(1L, it.canonicalFitGenerationObservations)
                assertEquals(1L, it.canonicalActiveFitGenerations)
                DatabaseBackup.restore(context, plan)
            }
            CirqaDatabase(context).use { database ->
                val restoredCount = database.readableDatabase.rawQuery(
                    "SELECT COUNT(*) FROM fit_generation_observations WHERE timestamp_origin = 'UNKNOWN'",
                    null
                ).use { it.moveToFirst(); it.getLong(0) }
                assertEquals(1L, restoredCount)
            }
            val restoredPage = HistoricalObservationRepository(context).read(
                HistoricalObservationQuery(
                    startInclusive = Instant.EPOCH,
                    endExclusive = Instant.ofEpochMilli(Long.MAX_VALUE),
                    limit = 20
                )
            )
            assertEquals(1, restoredPage.observations.size)
            assertEquals("UNKNOWN", restoredPage.observations.single().provenance?.timestampOrigin)
        } finally {
            root.deleteRecursively()
        }
    }

    @Test fun schemaFiveUpgradeAddsInventoryWithoutChangingExistingFitSources() {
        val base = ApplicationProvider.getApplicationContext<Context>()
        val root = Files.createTempDirectory(base.cacheDir.toPath(), "fit-inventory-migration-").toFile()
        val context = isolatedContext(base, root)
        try {
            val legacy = SQLiteDatabase.openOrCreateDatabase(
                context.getDatabasePath("cirqa_measurements.db"), null
            )
            legacy.execSQL(
                """CREATE TABLE fit_sources (
                    source_sha256 TEXT PRIMARY KEY NOT NULL CHECK(length(source_sha256) = 64),
                    file_bytes INTEGER NOT NULL, relative_path TEXT NOT NULL,
                    validated_at_ms INTEGER NOT NULL, decoder_version TEXT NOT NULL,
                    profile_version TEXT NOT NULL
                )"""
            )
            legacy.execSQL(
                """INSERT INTO fit_sources VALUES (?, 100, ?, 10, '1', '21.222.0')""",
                arrayOf("e".repeat(64), "verified_fit/${"e".repeat(64)}.fit")
            )
            legacy.execSQL(
                """CREATE TABLE observations (
                    device_id TEXT NOT NULL, metric TEXT NOT NULL, measured_at_ms INTEGER NOT NULL,
                    value REAL NOT NULL, unit TEXT NOT NULL, sample_id TEXT NOT NULL,
                    received_at_ms INTEGER
                )"""
            )
            legacy.execSQL(
                """CREATE TABLE fit_observation_provenance (
                    sample_id TEXT NOT NULL, device_id TEXT NOT NULL, metric TEXT NOT NULL,
                    measured_at_ms INTEGER NOT NULL, source_sha256 TEXT NOT NULL,
                    record_ordinal INTEGER NOT NULL, global_message_number INTEGER NOT NULL,
                    field_number INTEGER NOT NULL, quality TEXT NOT NULL, decoder_version TEXT NOT NULL
                )"""
            )
            legacy.version = 5
            legacy.close()

            CirqaDatabase(context).use { database ->
                assertEquals(11, database.readableDatabase.version)
                val sourceCount = database.readableDatabase.rawQuery(
                    "SELECT COUNT(*) FROM fit_sources", null
                ).use { it.moveToFirst(); it.getInt(0) }
                assertEquals(1, sourceCount)
                val inventoryTableExists = database.readableDatabase.rawQuery(
                    "SELECT COUNT(*) FROM sqlite_master WHERE type = 'table' AND name = 'fit_unmapped_fields'",
                    null
                ).use { it.moveToFirst(); it.getInt(0) }
                assertEquals(1, inventoryTableExists)
            }
        } finally {
            root.deleteRecursively()
        }
    }

    @Test fun fitFieldInventoryReplayPreservesReviewStatusAndBackupRestore() {
        val base = ApplicationProvider.getApplicationContext<Context>()
        val root = Files.createTempDirectory(base.cacheDir.toPath(), "fit-inventory-db-test-").toFile()
        val context = isolatedContext(base, root)
        val sourceSha = "d".repeat(64)
        val field = FitFieldInventoryRecord(
            sourceSha256 = sourceSha,
            globalMessageNumber = 20,
            kind = StoredFitFieldKind.STANDARD,
            fieldNumber = 85,
            baseTypeCode = 2,
            fieldSize = 1,
            developerDataIndex = null,
            classification = "FIELD_NOT_DECODED_BY_APP",
            definitionCount = 1,
            observedMessageCount = 3,
            validElementCount = 2,
            invalidElementCount = 1,
            decoderVersion = FitHistoricalObservations.DECODER_VERSION,
            profileVersion = FitHistoricalObservations.PROFILE_VERSION
        )
        try {
            CirqaDatabase(context).use { database ->
                database.commitFitFile(
                    sourceSha256 = sourceSha,
                    fileBytes = 100,
                    relativePath = "verified_fit/$sourceSha.fit",
                    validatedAtEpochMillis = 10,
                    decoderVersion = FitHistoricalObservations.DECODER_VERSION,
                    profileVersion = FitHistoricalObservations.PROFILE_VERSION,
                    observations = emptyList(),
                    unmappedFields = listOf(field)
                )
                val initial = database.storedFitUnmappedFields().single()
                assertEquals(FitFieldInventoryStatus.UNREVIEWED, initial.status)
                database.updateFitUnmappedFieldStatus(
                    initial, FitFieldInventoryStatus.INVESTIGATING
                )
                database.commitFitFile(
                    sourceSha256 = sourceSha,
                    fileBytes = 100,
                    relativePath = "verified_fit/$sourceSha.fit",
                    validatedAtEpochMillis = 20,
                    decoderVersion = FitHistoricalObservations.DECODER_VERSION,
                    profileVersion = FitHistoricalObservations.PROFILE_VERSION,
                    observations = emptyList(),
                    unmappedFields = listOf(field.copy(observedMessageCount = 4, validElementCount = 3))
                )
                val replay = database.storedFitUnmappedFields().single()
                assertEquals(4L, replay.observedMessageCount)
                assertEquals(3L, replay.validElementCount)
                assertEquals(FitFieldInventoryStatus.INVESTIGATING, replay.status)
            }

            val archive = ByteArrayOutputStream()
            DatabaseBackup.write(context, archive)
            val plan = DatabaseBackup.prepareRestore(context, ByteArrayInputStream(archive.toByteArray()))
            plan.use {
                DatabaseBackup.restore(context, plan)
            }
            CirqaDatabase(context).use { database ->
                val restored = database.storedFitUnmappedFields().single()
                assertEquals(4L, restored.observedMessageCount)
                assertEquals(FitFieldInventoryStatus.INVESTIGATING, restored.status)
            }
        } finally {
            root.deleteRecursively()
        }
    }

    @Test fun backupResetAndRestoreRoundTripBothLocalStoresAndCursor() {
        val base = ApplicationProvider.getApplicationContext<Context>()
        val root = Files.createTempDirectory(base.cacheDir.toPath(), "database-backup-test-").toFile()
        val context = isolatedContext(base, root)
        try {
            val event = """{"schema":1,"type":"reading","received_at_utc":"2026-11-17T08:00:00Z","service":6,"bpm":71}"""
            CirqaDatabase(context).use { assertTrue(it.insertLine(event)) }
            val historical = RawExportRecord("test-device", "spo2", 2_000, 97.0, "%", "sample", 3_000)
            val sourceSha = "a".repeat(64)
            val fitObservation = FitHistoricalObservations.Observation(
                metric = FitHistoricalObservations.Metric.HEART_RATE,
                timestampEpochMillis = 4_000,
                value = 65.0,
                unit = "bpm",
                sourceFileSha256 = sourceSha,
                recordOrdinal = 1,
                globalMessageNumber = 20,
                fieldNumber = 3,
                quality = FitHistoricalObservations.Quality.PROFILE_VALID,
                idempotencyKey = "fit-sample",
                duplicateFingerprint = "semantic-fingerprint"
            )
            val legacyUntrustedSpo2 = FitHistoricalObservations.ReviewCandidate(
                profileFieldName = "reading_spo2",
                value = 98.0,
                unit = "percent",
                timestampEpochMillis = null,
                sourceFileSha256 = sourceSha,
                recordOrdinal = 2,
                globalMessageNumber = 269,
                fieldNumber = 0,
                timestampOrigin = FitHistoricalObservations.TimestampOrigin.UNKNOWN
            )
            LocalReadingStore(context).use {
                it.commitBatch("test-device", listOf(historical))
                it.commitFitFile(
                    sourceSha256 = sourceSha,
                    fileBytes = 100,
                    relativePath = "verified_fit/$sourceSha.fit",
                    validatedAtEpochMillis = 5_000,
                    decoderVersion = FitHistoricalObservations.DECODER_VERSION,
                    profileVersion = FitHistoricalObservations.PROFILE_VERSION,
                    observations = listOf(fitObservation),
                    reviewCandidates = listOf(legacyUntrustedSpo2)
                )
            }
            val canonicalSha = "b".repeat(64)
            val canonicalObservation = fitObservation.copy(
                timestampEpochMillis = 6_000,
                value = 67.0,
                sourceFileSha256 = canonicalSha,
                recordOrdinal = 2,
                globalMessageNumber = 21,
                idempotencyKey = "canonical-only-sample",
                duplicateFingerprint = "canonical-only-fingerprint"
            )
            val untrustedStress = FitHistoricalObservations.ReviewCandidate(
                profileFieldName = "stress_level_value",
                value = 42.0,
                unit = "profile_raw",
                timestampEpochMillis = null,
                sourceFileSha256 = canonicalSha,
                recordOrdinal = 3,
                globalMessageNumber = 227,
                fieldNumber = 0,
                timestampOrigin = FitHistoricalObservations.TimestampOrigin.UNKNOWN
            )
            val canonicalMetadata = FitHistoricalObservations.DeviceMetadata(
                sourceFileSha256 = canonicalSha,
                recordOrdinal = 1,
                timestampEpochMillis = 6_000,
                deviceTypeCode = 4,
                manufacturerCode = 1,
                productCode = 2,
                softwareVersion = 12.34,
                batteryVoltageVolts = null,
                batteryStatusCode = null,
                batteryLevelPercent = null
            )
            val rawMetadata = RawDeviceMetadataRecord(
                sourceSha256 = "c".repeat(64),
                fileBytes = 20_000,
                verifiedAtEpochMillis = 8_000,
                parserVersion = CirqaRawDeviceMetadataParser.VERSION,
                softwareVersion = "1.2.3",
                massStorageMode = false,
                transferDirectionElementCount = 51
            )
            CirqaDatabase(context).use { database ->
                database.ensureHistoricalStoreCopied()
                database.commitFitFile(
                    sourceSha256 = canonicalSha,
                    fileBytes = 101,
                    relativePath = "verified_fit/$canonicalSha.fit",
                    validatedAtEpochMillis = 7_000,
                    decoderVersion = FitHistoricalObservations.DECODER_VERSION,
                    profileVersion = FitHistoricalObservations.PROFILE_VERSION,
                    observations = listOf(canonicalObservation),
                    deviceMetadata = listOf(canonicalMetadata),
                    reviewCandidates = listOf(untrustedStress),
                    decodedFields = listOf(FitHistoricalObservations.DecodedField(
                        sourceFileSha256 = canonicalSha, recordOrdinal = 1,
                        globalMessageNumber = 20, fieldNumber = 99, fieldKind = "STANDARD",
                        baseTypeCode = 2, fieldSize = 1, developerDataIndex = null,
                        bigEndian = false, rawBytes = byteArrayOf(0xa5.toByte()),
                        timestampEpochMillis = 6_000,
                        timestampOrigin = FitHistoricalObservations.TimestampOrigin.DIRECT
                    ))
                )
                database.storeRawDeviceMetadata(rawMetadata)
                database.storeRawDeviceMetadata(rawMetadata.copy(verifiedAtEpochMillis = 9_000))
            }

            val bytes = ByteArrayOutputStream()
            val report = DatabaseBackup.write(context, bytes)
            assertEquals(1L, report.events)
            assertEquals(2L, report.legacyReadings)
            assertEquals(2L, report.syncCursors)
            assertEquals(1L, report.fitSources)
            assertEquals(1L, report.fitProvenance)
            assertEquals(3L, report.canonicalReadings)
            assertEquals(2L, report.canonicalFitSources)
            assertEquals(2L, report.canonicalFitProvenance)
            assertEquals(1L, report.canonicalDeviceMetadata)
            assertEquals(1L, report.rawDeviceMetadata)
            assertEquals(2L, report.canonicalFitGenerations)
            assertEquals(2L, report.canonicalFitGenerationObservations)
            assertEquals(1L, report.canonicalFitGenerationFields)
            assertEquals(2L, report.canonicalActiveFitGenerations)
            assertEquals(1L, report.canonicalFitReviewCandidates)
            assertEquals(1L, report.legacyFitReviewCandidates)

            DatabaseBackup.reset(context)
            CirqaDatabase(context).use { assertEquals(0L, it.eventCount()) }
            LocalReadingStore(context).use {
                assertTrue(it.readWindow(ExportWindow(0, Long.MAX_VALUE)).isEmpty())
                assertNull(it.cursorFor("test-device").lastCommittedEpochMillis)
            }

            val plan = DatabaseBackup.prepareRestore(context, ByteArrayInputStream(bytes.toByteArray()))
            try {
                assertEquals(10, plan.version)
                assertEquals(3L, plan.canonicalReadings)
                assertEquals(1L, plan.canonicalDeviceMetadata)
                assertEquals(1L, plan.rawDeviceMetadata)
                assertEquals(1L, plan.canonicalFitReviewCandidates)
                assertEquals(1L, plan.canonicalFitGenerationFields)
                assertEquals(1L, plan.legacyFitReviewCandidates)
                DatabaseBackup.restore(context, plan)
            } finally { plan.close() }

            CirqaDatabase(context).use { db ->
                assertArrayEquals(byteArrayOf(0xa5.toByte()), db.readableDatabase.rawQuery(
                    "SELECT raw_bytes FROM fit_generation_fields WHERE field_number = 99", null
                ).use { assertTrue(it.moveToFirst()); it.getBlob(0) })
                assertEquals(1L, db.eventCount())
                assertEquals(listOf(event), db.allLines())
                val restored = db.readUntrustedFitCandidates().single()
                assertEquals("stress", restored.metric)
                assertEquals(42.0, restored.value, 0.0)
                assertNull(restored.measuredAtEpochMillis)
                assertEquals("UNTRUSTED_PROFILE_CANDIDATE", restored.quality)
                assertEquals("UNKNOWN", restored.timestampOrigin)
            }
            LocalReadingStore(context).use {
                val restored = it.readWindow(ExportWindow(0, Long.MAX_VALUE))
                assertEquals(2, restored.size)
                assertTrue(restored.contains(historical))
                assertTrue(restored.any { row -> row.deviceId == "cirqa-fit" && row.value == 65.0 })
                assertEquals(2_000L, it.cursorFor("test-device").lastCommittedEpochMillis)
                assertEquals(4_000L, it.cursorFor("cirqa-fit").lastCommittedEpochMillis)
                val db = it.readableDatabase
                assertEquals(1, db.rawQuery("SELECT COUNT(*) FROM fit_sources WHERE source_sha256 = ?", arrayOf(sourceSha)).use {
                    it.moveToFirst(); it.getInt(0)
                })
                assertEquals(1, db.rawQuery("SELECT COUNT(*) FROM fit_observation_provenance WHERE sample_id = 'fit-sample'", null).use {
                    it.moveToFirst(); it.getInt(0)
                })
                db.rawQuery(
                    """SELECT metric, measured_at_ms, value, unit, quality, timestamp_origin,
                              source_sha256, record_ordinal, global_message_number, field_number,
                              decoder_version
                       FROM fit_review_candidates WHERE sample_id = ?""",
                    arrayOf(legacyUntrustedSpo2.idempotencyKey)
                ).use { rows ->
                    assertTrue(rows.moveToFirst())
                    assertEquals("spo2", rows.getString(0))
                    assertTrue(rows.isNull(1))
                    assertEquals(98.0, rows.getDouble(2), 0.0)
                    assertEquals("percent", rows.getString(3))
                    assertEquals("UNTRUSTED_PROFILE_CANDIDATE", rows.getString(4))
                    assertEquals("UNKNOWN", rows.getString(5))
                    assertEquals(sourceSha, rows.getString(6))
                    assertEquals(2, rows.getInt(7))
                    assertEquals(269, rows.getInt(8))
                    assertEquals(0, rows.getInt(9))
                    assertEquals(FitHistoricalObservations.DECODER_VERSION, rows.getString(10))
                    assertFalse(rows.moveToNext())
                }
                assertEquals(0, db.rawQuery("PRAGMA foreign_key_check", null).use {
                    var violations = 0
                    while (it.moveToNext()) violations++
                    violations
                })
            }
            CirqaDatabase(context).use { database ->
                val db = database.readableDatabase
                assertEquals(3, db.rawQuery("SELECT COUNT(*) FROM observations", null).use {
                    it.moveToFirst(); it.getInt(0)
                })
                assertEquals(2, db.rawQuery("SELECT COUNT(*) FROM fit_sources", null).use {
                    it.moveToFirst(); it.getInt(0)
                })
                assertEquals(2, db.rawQuery("SELECT COUNT(*) FROM fit_observation_provenance", null).use {
                    it.moveToFirst(); it.getInt(0)
                })
                assertEquals(1, db.rawQuery(
                    "SELECT COUNT(*) FROM fit_device_metadata WHERE source_sha256 = ? AND software_version = 12.34",
                    arrayOf(canonicalSha)
                ).use { it.moveToFirst(); it.getInt(0) })
                assertEquals(listOf(canonicalMetadata), database.fitDeviceMetadata())
                assertEquals(listOf(rawMetadata), database.rawDeviceMetadata())
                assertTrue(db.rawQuery(
                    "SELECT value FROM observations WHERE sample_id = 'canonical-only-sample'", null
                ).use { it.moveToFirst() && it.getDouble(0) == 67.0 })
                assertEquals(0, db.rawQuery("PRAGMA foreign_key_check", null).use {
                    var violations = 0
                    while (it.moveToNext()) violations++
                    violations
                })
            }
        } finally {
            root.deleteRecursively()
        }
    }

    @Test fun version1BackupRemainsRestorableAfterProvenanceSchemaUpgrade() {
        val base = ApplicationProvider.getApplicationContext<Context>()
        val root = Files.createTempDirectory(base.cacheDir.toPath(), "database-backup-v1-test-").toFile()
        val context = isolatedContext(base, root)
        try {
            val sha = "b".repeat(64)
            LocalReadingStore(context).use {
                it.commitFitFile(
                    sourceSha256 = sha,
                    fileBytes = 80,
                    relativePath = "verified_fit/$sha.fit",
                    validatedAtEpochMillis = 6_000,
                    decoderVersion = FitHistoricalObservations.DECODER_VERSION,
                    profileVersion = FitHistoricalObservations.PROFILE_VERSION,
                    observations = listOf(FitHistoricalObservations.Observation(
                        metric = FitHistoricalObservations.Metric.HEART_RATE,
                        timestampEpochMillis = 7_000,
                        value = 70.0,
                        unit = "bpm",
                        sourceFileSha256 = sha,
                        recordOrdinal = 1,
                        globalMessageNumber = 20,
                        fieldNumber = 3,
                        quality = FitHistoricalObservations.Quality.PROFILE_VALID,
                        idempotencyKey = "old-fit-sample",
                        duplicateFingerprint = "old-fingerprint"
                    ))
                )
            }

            val plan = DatabaseBackup.prepareRestore(context, ByteArrayInputStream(emptyVersion1Backup()))
            try {
                assertEquals(1, plan.version)
                assertEquals(0L, plan.fitSources)
                DatabaseBackup.restore(context, plan)
            } finally { plan.close() }

            LocalReadingStore(context).use {
                assertTrue(it.readWindow(ExportWindow(0, Long.MAX_VALUE)).isEmpty())
                assertEquals(0, it.readableDatabase.rawQuery("SELECT COUNT(*) FROM fit_sources", null).use {
                    it.moveToFirst(); it.getInt(0)
                })
                assertEquals(0, it.readableDatabase.rawQuery(
                    "SELECT COUNT(*) FROM fit_observation_provenance", null
                ).use { it.moveToFirst(); it.getInt(0) })
            }
        } finally {
            root.deleteRecursively()
        }
    }

    @Test fun orphanedFitProvenanceIsRejectedBeforeRestoreConfirmation() {
        val base = ApplicationProvider.getApplicationContext<Context>()
        val root = Files.createTempDirectory(base.cacheDir.toPath(), "database-backup-fk-test-").toFile()
        val context = isolatedContext(base, root)
        val original = RawExportRecord("keep-device", "heart_rate", 123, 70.0, "bpm", "keep")
        try {
            LocalReadingStore(context).use { it.commitBatch("keep-device", listOf(original)) }
            try {
                DatabaseBackup.prepareRestore(context, ByteArrayInputStream(version2ArchiveWithOrphan()))
                fail("Expected orphaned source reference to be rejected")
            } catch (_: Exception) { }
            LocalReadingStore(context).use {
                assertEquals(listOf(original), it.readWindow(ExportWindow(0, 1000)))
            }
        } finally {
            root.deleteRecursively()
        }
    }

    @Test fun version2BackupRemainsRestorableAfterCanonicalWriteCutover() {
        val base = ApplicationProvider.getApplicationContext<Context>()
        val root = Files.createTempDirectory(base.cacheDir.toPath(), "database-backup-v2-test-").toFile()
        val context = isolatedContext(base, root)
        try {
            val plan = DatabaseBackup.prepareRestore(context, ByteArrayInputStream(emptyVersion2Backup()))
            try {
                assertEquals(2, plan.version)
                assertEquals(0L, plan.canonicalReadings)
                DatabaseBackup.restore(context, plan)
            } finally { plan.close() }

            CirqaDatabase(context).use { database ->
                assertEquals(0, database.readableDatabase.rawQuery(
                    "SELECT COUNT(*) FROM observations", null
                ).use { it.moveToFirst(); it.getInt(0) })
            }
        } finally {
            root.deleteRecursively()
        }
    }

    @Test fun version3BackupRemainsRestorableAfterMetadataSchemaUpgrade() {
        val base = ApplicationProvider.getApplicationContext<Context>()
        val root = Files.createTempDirectory(base.cacheDir.toPath(), "database-backup-v3-test-").toFile()
        val context = isolatedContext(base, root)
        try {
            val plan = DatabaseBackup.prepareRestore(context, ByteArrayInputStream(emptyVersion2Backup(version = 3)))
            try {
                assertEquals(3, plan.version)
                assertEquals(0L, plan.canonicalDeviceMetadata)
                DatabaseBackup.restore(context, plan)
            } finally { plan.close() }

            CirqaDatabase(context).use { database ->
                assertEquals(0, database.readableDatabase.rawQuery(
                    "SELECT COUNT(*) FROM fit_device_metadata", null
                ).use { it.moveToFirst(); it.getInt(0) })
            }
        } finally {
            root.deleteRecursively()
        }
    }

    @Test fun orphanedDeviceMetadataIsRejectedBeforeRestoreConfirmation() {
        val base = ApplicationProvider.getApplicationContext<Context>()
        val root = Files.createTempDirectory(base.cacheDir.toPath(), "database-backup-metadata-fk-test-").toFile()
        val context = isolatedContext(base, root)
        val sentinel = """{"schema":1,"type":"attempt","received_at_utc":"2026-11-18T00:00:00Z"}"""
        try {
            CirqaDatabase(context).use { assertTrue(it.insertLine(sentinel)) }
            try {
                DatabaseBackup.prepareRestore(
                    context,
                    ByteArrayInputStream(version4ArchiveWithOrphanedDeviceMetadata())
                )
                fail("Expected orphaned FIT device metadata to be rejected")
            } catch (_: Exception) { }
            CirqaDatabase(context).use { assertEquals(listOf(sentinel), it.allLines()) }
        } finally {
            root.deleteRecursively()
        }
    }

    @Test fun version4BackupRestoresWithoutRawMetadataEntryAfterSchemaV5Upgrade() {
        val base = ApplicationProvider.getApplicationContext<Context>()
        val root = Files.createTempDirectory(base.cacheDir.toPath(), "database-backup-v4-restore-test-").toFile()
        val context = isolatedContext(base, root)
        val sentinel = """{"schema":1,"type":"attempt","received_at_utc":"2026-11-18T00:00:00Z"}"""
        try {
            CirqaDatabase(context).use { assertTrue(it.insertLine(sentinel)) }
            val plan = DatabaseBackup.prepareRestore(
                context,
                ByteArrayInputStream(version4ArchiveWithoutRawMetadata())
            )
            try {
                assertEquals(4, plan.version)
                assertEquals(0L, plan.rawDeviceMetadata)
                DatabaseBackup.restore(context, plan)
            } finally {
                plan.close()
            }
            CirqaDatabase(context).use { database ->
                assertTrue(database.allLines().isEmpty())
                assertTrue(database.rawDeviceMetadata().isEmpty())
            }
        } finally {
            root.deleteRecursively()
        }
    }

    private fun emptyVersion1Backup(): ByteArray {
        val dataEntries = listOf("cirqa_events.jsonl", "legacy_readings.jsonl", "sync_cursors.jsonl")
        val emptySha = MessageDigest.getInstance("SHA-256").digest(byteArrayOf())
            .joinToString("") { "%02x".format(it.toInt() and 255) }
        val entries = JSONObject()
        dataEntries.forEach { name ->
            entries.put(name, JSONObject().put("records", 0).put("bytes", 0).put("sha256", emptySha))
        }
        val manifest = JSONObject()
            .put("format", "de.pulselibre.local-database-backup")
            .put("version", 1)
            .put("app_id", "de.pulselibre")
            .put("created_at", "2026-11-18T00:00:00Z")
            .put("encryption", "none")
            .put("entries", entries)
        return ByteArrayOutputStream().also { bytes ->
            ZipOutputStream(bytes).use { zip ->
                (dataEntries + "manifest.json").forEach { name ->
                    zip.putNextEntry(ZipEntry(name))
                    if (name == "manifest.json") zip.write(manifest.toString().toByteArray(Charsets.UTF_8))
                    zip.closeEntry()
                }
            }
        }.toByteArray()
    }

    private fun emptyVersion2Backup(version: Int = 2): ByteArray {
        val dataEntries = if (version == 2) listOf(
            "cirqa_events.jsonl", "legacy_readings.jsonl", "sync_cursors.jsonl",
            "fit_sources.jsonl", "fit_observation_provenance.jsonl"
        ) else listOf(
            "cirqa_events.jsonl", "legacy_readings.jsonl", "sync_cursors.jsonl",
            "fit_sources.jsonl", "fit_observation_provenance.jsonl",
            "canonical_observations.jsonl", "canonical_sync_cursors.jsonl",
            "canonical_fit_sources.jsonl", "canonical_fit_observation_provenance.jsonl"
        )
        val emptySha = MessageDigest.getInstance("SHA-256").digest(byteArrayOf())
            .joinToString("") { "%02x".format(it.toInt() and 255) }
        val entries = JSONObject()
        dataEntries.forEach { name ->
            entries.put(name, JSONObject().put("records", 0).put("bytes", 0).put("sha256", emptySha))
        }
        val manifest = JSONObject()
            .put("format", "de.pulselibre.local-database-backup")
            .put("version", version)
            .put("app_id", "de.pulselibre")
            .put("created_at", "2026-11-18T00:00:00Z")
            .put("encryption", "none")
            .put("entries", entries)
        return ByteArrayOutputStream().also { bytes ->
            ZipOutputStream(bytes).use { zip ->
                (dataEntries + "manifest.json").forEach { name ->
                    zip.putNextEntry(ZipEntry(name))
                    if (name == "manifest.json") zip.write(manifest.toString().toByteArray(Charsets.UTF_8))
                    zip.closeEntry()
                }
            }
        }.toByteArray()
    }

    private fun version2ArchiveWithOrphan(): ByteArray {
        val payloads = linkedMapOf(
            "cirqa_events.jsonl" to "",
            "legacy_readings.jsonl" to "",
            "sync_cursors.jsonl" to "",
            "fit_sources.jsonl" to "",
            "fit_observation_provenance.jsonl" to
                """{"sample_id":"orphan","device_id":"cirqa-fit","metric":"heart_rate","measured_at_ms":5000,"source_sha256":"${"c".repeat(64)}","record_ordinal":1,"global_message_number":20,"field_number":3,"quality":"PROFILE_VALID","decoder_version":"1"}""" + "\n"
        )
        val metadata = JSONObject()
        payloads.forEach { (name, value) ->
            val bytes = value.toByteArray(Charsets.UTF_8)
            metadata.put(name, JSONObject()
                .put("records", if (value.isEmpty()) 0 else 1)
                .put("bytes", bytes.size)
                .put("sha256", MessageDigest.getInstance("SHA-256").digest(bytes)
                    .joinToString("") { "%02x".format(it.toInt() and 255) }))
        }
        val manifest = JSONObject()
            .put("format", "de.pulselibre.local-database-backup")
            .put("version", 2)
            .put("app_id", "de.pulselibre")
            .put("created_at", "2026-11-18T00:00:00Z")
            .put("encryption", "none")
            .put("entries", metadata)
        return ByteArrayOutputStream().also { bytes ->
            ZipOutputStream(bytes).use { zip ->
                payloads.forEach { (name, value) ->
                    zip.putNextEntry(ZipEntry(name))
                    zip.write(value.toByteArray(Charsets.UTF_8))
                    zip.closeEntry()
                }
                zip.putNextEntry(ZipEntry("manifest.json"))
                zip.write(manifest.toString().toByteArray(Charsets.UTF_8))
                zip.closeEntry()
            }
        }.toByteArray()
    }

    private fun version4ArchiveWithOrphanedDeviceMetadata(): ByteArray =
        version4Archive(includeOrphanedFitMetadata = true)

    private fun version4ArchiveWithoutRawMetadata(): ByteArray =
        version4Archive(includeOrphanedFitMetadata = false)

    private fun version4Archive(includeOrphanedFitMetadata: Boolean): ByteArray {
        val payloads = linkedMapOf(
            "cirqa_events.jsonl" to "",
            "legacy_readings.jsonl" to "",
            "sync_cursors.jsonl" to "",
            "fit_sources.jsonl" to "",
            "fit_observation_provenance.jsonl" to "",
            "canonical_observations.jsonl" to "",
            "canonical_sync_cursors.jsonl" to "",
            "canonical_fit_sources.jsonl" to "",
            "canonical_fit_observation_provenance.jsonl" to "",
            "canonical_fit_device_metadata.jsonl" to if (includeOrphanedFitMetadata)
                """{"source_sha256":"${"c".repeat(64)}","record_ordinal":1,"observed_at_ms":null,"device_type_code":null,"manufacturer_code":null,"product_code":null,"software_version":null,"battery_voltage_volts":null,"battery_status_code":null,"battery_level_percent":null}""" + "\n"
            else ""
        )
        val metadata = JSONObject()
        payloads.forEach { (name, value) ->
            val bytes = value.toByteArray(Charsets.UTF_8)
            metadata.put(name, JSONObject()
                .put("records", if (value.isEmpty()) 0 else 1)
                .put("bytes", bytes.size)
                .put("sha256", MessageDigest.getInstance("SHA-256").digest(bytes)
                    .joinToString("") { "%02x".format(it.toInt() and 255) }))
        }
        val manifest = JSONObject()
            .put("format", "de.pulselibre.local-database-backup")
            .put("version", 4)
            .put("app_id", "de.pulselibre")
            .put("created_at", "2026-11-18T00:00:00Z")
            .put("encryption", "none")
            .put("entries", metadata)
        return ByteArrayOutputStream().also { bytes ->
            ZipOutputStream(bytes).use { zip ->
                payloads.forEach { (name, value) ->
                    zip.putNextEntry(ZipEntry(name))
                    zip.write(value.toByteArray(Charsets.UTF_8))
                    zip.closeEntry()
                }
                zip.putNextEntry(ZipEntry("manifest.json"))
                zip.write(manifest.toString().toByteArray(Charsets.UTF_8))
                zip.closeEntry()
            }
        }.toByteArray()
    }

    @Test fun version7BackupRemainsRestorableAfterLiveMetadataSchemaUpgrade() {
        val base = ApplicationProvider.getApplicationContext<Context>()
        val root = Files.createTempDirectory(base.cacheDir.toPath(), "backup-v7-compat-").toFile()
        val context = isolatedContext(base, root)
        try {
            val plan = DatabaseBackup.prepareRestore(context, ByteArrayInputStream(emptyVersionedBackup(7)))
            plan.use {
                assertEquals(7, it.version)
                assertEquals(0L, it.liveDeviceMetadata)
                DatabaseBackup.restore(context, it)
            }
            CirqaDatabase(context).use { db ->
                assertEquals(11, db.readableDatabase.version)
                assertTrue(db.latestLiveDeviceMetadata().isEmpty())
            }
        } finally {
            root.deleteRecursively()
        }
    }

    @Test fun liveDeviceMetadataSurvivesBackupRoundTrip() {
        val base = ApplicationProvider.getApplicationContext<Context>()
        val root = Files.createTempDirectory(base.cacheDir.toPath(), "backup-v8-live-metadata-").toFile()
        val context = isolatedContext(base, root)
        val observed = 1_800_000_000_000L
        try {
            CirqaDatabase(context).use { db ->
                db.storeLiveDeviceMetadata(listOf(
                    LiveDeviceMetadata(
                        DeviceMetadataFields.BATTERY_LEVEL, "37", "%",
                        DeviceMetadataSources.GARMIN_GDI_STATUS, observed
                    ),
                    LiveDeviceMetadata(
                        DeviceMetadataFields.FIRMWARE_VERSION, "1.2.3", null,
                        DeviceMetadataSources.GATT_DEVICE_INFO, observed
                    )
                ), storedAtMs = observed + 1)
            }
            val archive = ByteArrayOutputStream()
            DatabaseBackup.write(context, archive)
            val plan = DatabaseBackup.prepareRestore(context, ByteArrayInputStream(archive.toByteArray()))
            plan.use {
                assertEquals(10, it.version)
                assertEquals(2L, it.liveDeviceMetadata)
                // Restore into a fresh store to prove the entry is actually replayed.
                val freshRoot = Files.createTempDirectory(base.cacheDir.toPath(), "backup-v8-restore-").toFile()
                try {
                    val freshContext = isolatedContext(base, freshRoot)
                    DatabaseBackup.restore(freshContext, it)
                    CirqaDatabase(freshContext).use { db ->
                        val values = db.latestLiveDeviceMetadata()
                        assertEquals(2, values.size)
                        assertTrue(values.any { v ->
                            v.fieldKey == DeviceMetadataFields.BATTERY_LEVEL && v.valueText == "37" &&
                                v.unit == "%" && v.source == DeviceMetadataSources.GARMIN_GDI_STATUS &&
                                v.observedAtMs == observed
                        })
                        assertTrue(values.any { v ->
                            v.fieldKey == DeviceMetadataFields.FIRMWARE_VERSION && v.valueText == "1.2.3"
                        })
                    }
                } finally {
                    freshRoot.deleteRecursively()
                }
            }
        } finally {
            root.deleteRecursively()
        }
    }

    /** Empty archive of a given legacy version; v7 deliberately has no live metadata entry. */
    private fun emptyVersionedBackup(version: Int): ByteArray {
        val dataEntries = mutableListOf(
            "cirqa_events.jsonl", "legacy_readings.jsonl", "sync_cursors.jsonl",
            "fit_sources.jsonl", "fit_observation_provenance.jsonl",
            "canonical_observations.jsonl", "canonical_sync_cursors.jsonl",
            "canonical_fit_sources.jsonl", "canonical_fit_observation_provenance.jsonl",
            "canonical_fit_device_metadata.jsonl", "raw_device_metadata.jsonl",
            "canonical_fit_unmapped_fields.jsonl", "canonical_fit_decoder_generations.jsonl",
            "canonical_fit_generation_observations.jsonl", "canonical_fit_active_generations.jsonl"
        )
        if (version >= 8) dataEntries += listOf(
            "canonical_fit_review_candidates.jsonl",
            "legacy_fit_review_candidates.jsonl",
            "live_device_metadata.jsonl"
        )
        if (version >= 9) dataEntries += "downloaded_file_archive.jsonl"
        if (version >= 10) dataEntries += "canonical_fit_generation_fields.jsonl"
        val emptySha = MessageDigest.getInstance("SHA-256").digest(byteArrayOf())
            .joinToString("") { "%02x".format(it.toInt() and 255) }
        val entries = JSONObject()
        dataEntries.forEach { name ->
            entries.put(name, JSONObject().put("records", 0).put("bytes", 0).put("sha256", emptySha))
        }
        val manifest = JSONObject()
            .put("format", "de.pulselibre.local-database-backup")
            .put("version", version)
            .put("app_id", "de.pulselibre")
            .put("created_at", "2026-11-18T00:00:00Z")
            .put("encryption", "none")
            .put("entries", entries)
        return ByteArrayOutputStream().also { bytes ->
            ZipOutputStream(bytes).use { zip ->
                (dataEntries + "manifest.json").forEach { name ->
                    zip.putNextEntry(ZipEntry(name))
                    if (name == "manifest.json") zip.write(manifest.toString().toByteArray(Charsets.UTF_8))
                    zip.closeEntry()
                }
            }
        }.toByteArray()
    }

    @Test fun invalidArchiveIsRejectedBeforeAnyStoredDataChanges() {
        val base = ApplicationProvider.getApplicationContext<Context>()
        val root = Files.createTempDirectory(base.cacheDir.toPath(), "database-backup-invalid-test-").toFile()
        val context = isolatedContext(base, root)
        try {
            val event = """{"schema":1,"type":"attempt","received_at_utc":"2026-11-17T08:00:00Z"}"""
            CirqaDatabase(context).use { assertTrue(it.insertLine(event)) }
            assertThrows(IllegalArgumentException::class.java) {
                DatabaseBackup.prepareRestore(context, ByteArrayInputStream("not a zip".toByteArray()))
            }
            CirqaDatabase(context).use { assertEquals(listOf(event), it.allLines()) }
        } finally {
            root.deleteRecursively()
        }
    }

    private fun isolatedContext(base: Context, root: File): Context = object : ContextWrapper(base) {
        override fun getApplicationContext(): Context = this
        override fun getFilesDir(): File = root
        override fun getDatabasePath(name: String): File = File(root, name)
        override fun openOrCreateDatabase(name: String, mode: Int,
            factory: SQLiteDatabase.CursorFactory?): SQLiteDatabase =
            openOrCreateDatabase(name, mode, factory, null)
        override fun openOrCreateDatabase(name: String, mode: Int,
            factory: SQLiteDatabase.CursorFactory?, errorHandler: DatabaseErrorHandler?): SQLiteDatabase =
            SQLiteDatabase.openOrCreateDatabase(getDatabasePath(name).absolutePath, factory, errorHandler)
    }
}
