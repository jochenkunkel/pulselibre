package de.pulselibre

import android.content.Context
import android.content.ContextWrapper
import android.database.DatabaseErrorHandler
import android.database.sqlite.SQLiteDatabase
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
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
}
