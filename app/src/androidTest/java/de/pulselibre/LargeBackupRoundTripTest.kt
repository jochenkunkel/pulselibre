package de.pulselibre

import android.content.Context
import android.content.ContextWrapper
import android.database.DatabaseErrorHandler
import android.database.sqlite.SQLiteDatabase
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.nio.file.Files
import java.util.zip.ZipFile

/**
 * Backup and restore with realistic data volume (P1, 25.11.2026). The S10 held 316,455 FIT field rows and 139.6 MiB of
 * uncompressed backup content; the old 128 MiB check made every backup fail (0-byte files) and the restore check
 * rejected sleep_level candidates. The small fixtures of [DatabaseBackupTest] could not see either. This test builds a
 * synthetic store of the same order of magnitude (all values synthetic) and requires a full round trip.
 */
@LargeTest
@RunWith(AndroidJUnit4::class)
class LargeBackupRoundTripTest {
    private val sources = 12
    private val fieldsPerSource = 30_000
    private val observationsPerSource = 5_000
    private val candidateFields = listOf("reading_spo2", "reading_confidence", "spo2_measurement_mode", "stress_level_value",
        "respiration_rate", "sleep_level", "monitoring_cycles", "monitoring_activity_type")

    @Test fun realisticVolumeSurvivesBackupValidationAndRestore() {
        val base = ApplicationProvider.getApplicationContext<Context>()
        val root = Files.createTempDirectory(base.cacheDir.toPath(), "large-backup-").toFile()
        val context = isolated(base, root)
        val archive = File(root, "backup.zip")
        try {
            CirqaDatabase(context).use { db ->
                db.ensureHistoricalStoreCopied()
                for (s in 0 until sources) commitSource(db, s)
            }
            val t0 = System.nanoTime()
            val report = archive.outputStream().use { DatabaseBackup.write(context, it) }
            val writeMs = (System.nanoTime() - t0) / 1_000_000
            assertEquals((sources * fieldsPerSource).toLong(), report.canonicalFitGenerationFields)
            assertEquals((sources * observationsPerSource).toLong(), report.canonicalReadings)

            // The content really is above the former limit, so this test would have caught the 0-byte backups.
            val uncompressed = ZipFile(archive).use { zip -> zip.entries().asSequence().sumOf { it.size } }
            assertTrue("only ${uncompressed / 1_048_576} MiB uncompressed", uncompressed > 128L * 1024 * 1024)
            assertTrue(archive.length() < uncompressed)

            val t1 = System.nanoTime()
            val plan = archive.inputStream().use { DatabaseBackup.prepareRestore(context, it) }
            val validateMs = (System.nanoTime() - t1) / 1_000_000
            assertEquals((sources * fieldsPerSource).toLong(), plan.canonicalFitGenerationFields)
            assertEquals((sources * candidateFields.size).toLong(), plan.canonicalFitReviewCandidates)
            DatabaseBackup.reset(context)
            val t2 = System.nanoTime()
            plan.use { DatabaseBackup.restore(context, it) }
            val restoreMs = (System.nanoTime() - t2) / 1_000_000

            CirqaDatabase(context).use { db ->
                fun count(table: String) = db.readableDatabase.rawQuery("SELECT COUNT(*) FROM $table", null)
                    .use { it.moveToFirst(); it.getLong(0) }
                assertEquals((sources * fieldsPerSource).toLong(), count("fit_generation_fields"))
                assertEquals((sources * observationsPerSource).toLong(), count("fit_generation_observations"))
                assertEquals((sources * candidateFields.size).toLong(), count("fit_review_candidates"))
                assertEquals(sources.toLong(), count("fit_active_generations"))
            }
            // Durations are reported for the log; a generous bound only catches pathological slowdowns.
            val summary = "backup ${writeMs} ms, validation ${validateMs} ms, restore ${restoreMs} ms, " +
                "${uncompressed / 1_048_576} MiB content, ${archive.length() / 1_048_576} MiB ZIP"
            println("LargeBackupRoundTripTest: $summary") // test output only, not production code
            assertTrue(summary, writeMs + validateMs + restoreMs < 10 * 60_000L)
        } finally { root.deleteRecursively() }
    }

    private fun commitSource(db: CirqaDatabase, s: Int) {
        val sha = "%064x".format(s + 1)
        val t0 = 1_790_000_000_000L + s * 86_400_000L
        val observations = (0 until observationsPerSource).map { i ->
            FitHistoricalObservations.Observation(
                metric = FitHistoricalObservations.Metric.HEART_RATE, timestampEpochMillis = t0 + i * 15_000L,
                value = 55.0 + (i % 60), unit = "bpm", sourceFileSha256 = sha, recordOrdinal = i + 1,
                globalMessageNumber = if (i % 4 == 0) 20 else 55, fieldNumber = if (i % 4 == 0) 3 else 27,
                quality = FitHistoricalObservations.Quality.PROFILE_VALID,
                timestampOrigin = FitHistoricalObservations.TimestampOrigin.DIRECT,
                idempotencyKey = "large-$s-$i", duplicateFingerprint = "large-fp-$s-$i"
            )
        }
        val fields = (0 until fieldsPerSource).map { i ->
            FitHistoricalObservations.DecodedField(
                sourceFileSha256 = sha, recordOrdinal = i / 5 + 1, globalMessageNumber = 55, fieldNumber = i % 5,
                fieldKind = "STANDARD", baseTypeCode = 2, fieldSize = 4, developerDataIndex = null, bigEndian = false,
                rawBytes = byteArrayOf(i.toByte(), (i shr 8).toByte(), s.toByte(), 0x5a),
                timestampEpochMillis = t0 + (i / 5) * 60_000L, timestampOrigin = FitHistoricalObservations.TimestampOrigin.DIRECT
            )
        }
        val candidates = candidateFields.mapIndexed { i, name ->
            FitHistoricalObservations.ReviewCandidate(
                profileFieldName = name, value = 1.0 + i, unit = "profile_raw", timestampEpochMillis = t0 + i,
                sourceFileSha256 = sha, recordOrdinal = i + 1, globalMessageNumber = 200 + i, fieldNumber = 0,
                timestampOrigin = FitHistoricalObservations.TimestampOrigin.DIRECT
            )
        }
        db.commitFitFile(
            sourceSha256 = sha, fileBytes = 400_000, relativePath = "verified_fit/$sha.fit", validatedAtEpochMillis = t0,
            decoderVersion = FitHistoricalObservations.DECODER_VERSION, profileVersion = FitHistoricalObservations.PROFILE_VERSION,
            observations = observations, deviceMetadata = emptyList(), reviewCandidates = candidates, decodedFields = fields
        )
    }

    private fun isolated(base: Context, root: File): Context = object : ContextWrapper(base) {
        override fun getApplicationContext(): Context = this
        override fun getFilesDir(): File = root
        override fun getDatabasePath(name: String): File = File(root, name)
        override fun openOrCreateDatabase(name: String, mode: Int, factory: SQLiteDatabase.CursorFactory?): SQLiteDatabase =
            openOrCreateDatabase(name, mode, factory, null)
        override fun openOrCreateDatabase(name: String, mode: Int, factory: SQLiteDatabase.CursorFactory?,
                                          errorHandler: DatabaseErrorHandler?): SQLiteDatabase =
            SQLiteDatabase.openOrCreateDatabase(getDatabasePath(name).absolutePath, factory, errorHandler)
    }
}
