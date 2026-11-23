package de.pulselibre

import android.content.Context
import android.content.ContextWrapper
import android.database.DatabaseErrorHandler
import android.database.sqlite.SQLiteDatabase
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.nio.file.Files
import java.time.Instant

@RunWith(AndroidJUnit4::class)
class HistoricalObservationRepositoryTest {
    @Test fun pagesCanonicalObservationsWithStableOrderAndOptionalProvenance() {
        val base = ApplicationProvider.getApplicationContext<Context>()
        val root = Files.createTempDirectory(base.cacheDir.toPath(), "observation-repository-test-").toFile()
        val context = isolatedContext(base, root)
        val sha = "a".repeat(64)
        val start = Instant.ofEpochMilli(1_000)
        val end = Instant.ofEpochMilli(4_000)
        try {
            CirqaDatabase(context).use { database ->
                assertEquals(3, database.commitFitFile(
                    sourceSha256 = sha,
                    fileBytes = 128,
                    relativePath = "verified_fit/$sha.fit",
                    validatedAtEpochMillis = 10_000,
                    decoderVersion = FitHistoricalObservations.DECODER_VERSION,
                    profileVersion = FitHistoricalObservations.PROFILE_VERSION,
                    observations = listOf(
                        observation(sha, "hr-a", 1_000, 70.0, ordinal = 1),
                        observation(sha, "hr-b", 2_000, 72.0, ordinal = 2),
                        observation(sha, "hr-c", 2_000, 73.0, ordinal = 3)
                    ),
                    deviceKey = "repository-test-device"
                ))
            }

            val repository = HistoricalObservationRepository(context)
            val first = repository.read(HistoricalObservationQuery(
                startInclusive = start,
                endExclusive = end,
                metric = "heart_rate",
                deviceId = "repository-test-device",
                limit = 2
            ))
            assertEquals(listOf("hr-a", "hr-b"), first.observations.map { it.sampleId })
            assertEquals("heart_rate", first.observations.first().metric)
            assertEquals("bpm", first.observations.first().unit)
            assertEquals(sha, first.observations.first().provenance?.sourceFileSha256)
            assertEquals(1, first.observations.first().provenance?.recordOrdinal)
            assertNotNull(first.next)

            val second = repository.read(HistoricalObservationQuery(
                startInclusive = start,
                endExclusive = end,
                metric = "heart_rate",
                deviceId = "repository-test-device",
                limit = 2,
                after = first.next
            ))
            assertEquals(listOf("hr-c"), second.observations.map { it.sampleId })
            assertNull(second.next)
            assertEquals(Instant.ofEpochMilli(2_000), second.observations.single().measuredAt)
        } finally {
            root.deleteRecursively()
        }
    }

    @Test fun includesRowsWithoutFitProvenanceAndUsesHalfOpenTimeWindows() {
        val base = ApplicationProvider.getApplicationContext<Context>()
        val root = Files.createTempDirectory(base.cacheDir.toPath(), "observation-repository-legacy-").toFile()
        val context = isolatedContext(base, root)
        try {
            LocalReadingStore(context).use { store ->
                store.commitBatch("legacy-device", listOf(
                    RawExportRecord("legacy-device", "heart_rate", 2_000, 68.0, "bpm", "legacy-sample"),
                    RawExportRecord("legacy-device", "heart_rate", 3_000, 69.0, "bpm", "end-boundary")
                ))
            }
            val page = HistoricalObservationRepository(context).read(HistoricalObservationQuery(
                startInclusive = Instant.ofEpochMilli(2_000),
                endExclusive = Instant.ofEpochMilli(3_000),
                deviceId = "legacy-device"
            ))
            assertEquals(1, page.observations.size)
            assertEquals("legacy-sample", page.observations.single().sampleId)
            assertNull(page.observations.single().provenance)
            assertTrue(page.next == null)
        } finally {
            root.deleteRecursively()
        }
    }

    @Test fun replayStaysInactiveUntilExplicitDigestValidatedActivationAndCanRollBack() {
        val base = ApplicationProvider.getApplicationContext<Context>()
        val root = Files.createTempDirectory(base.cacheDir.toPath(), "fit-generation-replay-").toFile()
        val context = isolatedContext(base, root)
        val sha = "c".repeat(64)
        val start = Instant.ofEpochMilli(1_000)
        val end = Instant.ofEpochMilli(3_000)
        try {
            val original = observation(sha, "stable-sample", 2_000, 70.0, ordinal = 1)
            val replayed = original.copy(value = 75.0)
            val replayGenerationId = CirqaDatabase(context).use { database ->
                database.commitFitFile(
                    sourceSha256 = sha,
                    fileBytes = 128,
                    relativePath = "verified_fit/$sha.fit",
                    validatedAtEpochMillis = 10_000,
                    decoderVersion = "decoder-v1",
                    profileVersion = FitHistoricalObservations.PROFILE_VERSION,
                    observations = listOf(original),
                    deviceMetadata = emptyList(),
                    unmappedFields = emptyList(),
                    deviceKey = "replay-test-device"
                )
                database.storeFitDecoderReplay(
                    sourceSha256 = sha,
                    decoderVersion = "decoder-v2",
                    profileVersion = FitHistoricalObservations.PROFILE_VERSION,
                    replayedAtEpochMillis = 20_000,
                    observations = listOf(replayed),
                    deviceKey = "replay-test-device"
                )
            }

            val repository = HistoricalObservationRepository(context)
            fun activeValue() = repository.read(HistoricalObservationQuery(
                startInclusive = start,
                endExclusive = end,
                deviceId = "replay-test-device"
            )).observations.single().value
            assertEquals(70.0, activeValue(), 0.0)
            CirqaDatabase(context).use { database ->
                assertEquals(replayGenerationId, database.storeFitDecoderReplay(
                    sourceSha256 = sha,
                    decoderVersion = "decoder-v2",
                    profileVersion = FitHistoricalObservations.PROFILE_VERSION,
                    replayedAtEpochMillis = 30_000,
                    observations = listOf(replayed),
                    deviceKey = "replay-test-device"
                ))
            }
            assertEquals(70.0, activeValue(), 0.0)

            CirqaDatabase(context).use { database ->
                try {
                    database.activateFitDecoderGeneration("d".repeat(64), replayGenerationId)
                    throw AssertionError("activation must reject a generation from another source")
                } catch (_: IllegalStateException) {
                    // Expected; rejected activation must leave the active projection unchanged.
                }
                assertEquals(70.0, activeValue(), 0.0)
                database.activateFitDecoderGeneration(sha, replayGenerationId)
            }

            assertEquals(75.0, activeValue(), 0.0)

            // Explicit activation is symmetric: the immutable original generation stays a rollback target.
            val originalGenerationId = FitDecoderGeneration.id(
                sha, "decoder-v1", FitHistoricalObservations.PROFILE_VERSION
            )
            CirqaDatabase(context).use { database ->
                database.activateFitDecoderGeneration(sha, originalGenerationId)
            }
            assertEquals(70.0, activeValue(), 0.0)
        } finally {
            root.deleteRecursively()
        }
    }

    @Test fun verifiedByteReplayMustMatchARegisteredSourceAndDoesNotAutoActivate() {
        val base = ApplicationProvider.getApplicationContext<Context>()
        val root = Files.createTempDirectory(base.cacheDir.toPath(), "fit-verified-replay-").toFile()
        val context = isolatedContext(base, root)
        val bytes = emptyFitFile()
        val sha = sha256(bytes)
        val device = "verified-replay-test"
        try {
            CirqaDatabase(context).use { database ->
                database.commitFitFile(
                    sourceSha256 = sha,
                    fileBytes = bytes.size.toLong(),
                    relativePath = "verified_fit/$sha.fit",
                    validatedAtEpochMillis = 10_000,
                    decoderVersion = "decoder-before-replay",
                    profileVersion = FitHistoricalObservations.PROFILE_VERSION,
                    observations = emptyList(),
                    deviceMetadata = emptyList(),
                    unmappedFields = emptyList(),
                    deviceKey = device
                )
                val originalActive = database.readableDatabase.rawQuery(
                    "SELECT generation_id FROM fit_active_generations WHERE source_sha256 = ?",
                    arrayOf(sha)
                ).use { assertTrue(it.moveToFirst()); it.getString(0) }

                val replayId = database.replayVerifiedFitSource(
                    sourceBytes = bytes,
                    replayedAtEpochMillis = 20_000,
                    deviceKey = device
                )
                assertEquals(
                    FitDecoderGeneration.id(
                        sha, FitHistoricalObservations.DECODER_VERSION,
                        FitHistoricalObservations.PROFILE_VERSION
                    ),
                    replayId
                )
                assertNotEquals(originalActive, replayId)
                assertEquals(originalActive, database.readableDatabase.rawQuery(
                    "SELECT generation_id FROM fit_active_generations WHERE source_sha256 = ?",
                    arrayOf(sha)
                ).use { assertTrue(it.moveToFirst()); it.getString(0) })

                val corrupt = bytes.clone().also { it[it.lastIndex] = (it.last().toInt() xor 1).toByte() }
                try {
                    database.replayVerifiedFitSource(corrupt, 30_000, device)
                    throw AssertionError("corrupt FIT bytes must be rejected")
                } catch (_: IllegalStateException) {
                    // Expected: envelope/CRC validation fails before any DB write.
                }
            }
        } finally {
            root.deleteRecursively()
        }
    }

    private fun observation(
        sha: String,
        id: String,
        timestamp: Long,
        value: Double,
        ordinal: Int
    ) = FitHistoricalObservations.Observation(
        metric = FitHistoricalObservations.Metric.HEART_RATE,
        timestampEpochMillis = timestamp,
        value = value,
        unit = "bpm",
        sourceFileSha256 = sha,
        recordOrdinal = ordinal,
        globalMessageNumber = 20,
        fieldNumber = 3,
        quality = FitHistoricalObservations.Quality.PROFILE_VALID,
        idempotencyKey = id,
        duplicateFingerprint = "fingerprint-$id"
    )

    private fun isolatedContext(base: Context, root: File) = object : ContextWrapper(base) {
        override fun getApplicationContext(): Context = this
        override fun getFilesDir(): File = root
        override fun getDatabasePath(name: String): File = File(root, name).also { it.parentFile?.mkdirs() }
        override fun openOrCreateDatabase(
            name: String,
            mode: Int,
            factory: SQLiteDatabase.CursorFactory?
        ): SQLiteDatabase = openOrCreateDatabase(name, mode, factory, null)
        override fun openOrCreateDatabase(
            name: String,
            mode: Int,
            factory: SQLiteDatabase.CursorFactory?,
            errorHandler: DatabaseErrorHandler?
        ): SQLiteDatabase = SQLiteDatabase.openOrCreateDatabase(getDatabasePath(name).absolutePath, factory, errorHandler)
    }

    private fun emptyFitFile(): ByteArray {
        val file = ByteArray(16)
        file[0] = 14
        file[1] = 0x20
        file[2] = 0x64
        ".FIT".toByteArray().copyInto(file, 8)
        val headerCrc = FitFileHeader.crc16(file, 0, 12)
        file[12] = headerCrc.toByte()
        file[13] = (headerCrc ushr 8).toByte()
        val crc = FitFileHeader.crc16(file, 0, file.size - 2)
        file[file.size - 2] = crc.toByte()
        file[file.size - 1] = (crc ushr 8).toByte()
        return file
    }

    private fun sha256(bytes: ByteArray): String =
        java.security.MessageDigest.getInstance("SHA-256").digest(bytes)
            .joinToString("") { "%02x".format(it.toInt() and 0xff) }
}
