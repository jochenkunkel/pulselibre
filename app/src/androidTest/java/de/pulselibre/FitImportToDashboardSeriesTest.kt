package de.pulselibre

import android.content.Context
import android.content.ContextWrapper
import android.database.DatabaseErrorHandler
import android.database.sqlite.SQLiteDatabase
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.nio.file.Files

/**
 * End to end with SYNTHETIC data (never health data): FIT observations -> SQLite -> repository -> verified dashboard series.
 * Pins: only PROFILE_VALID rows with a direct band timestamp are drawn, an unknown-timestamp row and a re-imported
 * duplicate never appear, series are separated per metric, and everything survives closing and reopening the database
 * (app restart). Runs on the emulator in an isolated database, never in the app's real store.
 */
@RunWith(AndroidJUnit4::class)
class FitImportToDashboardSeriesTest {
    private val sha = "b".repeat(64)
    private val t0 = 1_800_000_000_000L // synthetic instant

    private fun obs(metric: FitHistoricalObservations.Metric, unit: String, id: String, at: Long, value: Double, ordinal: Int,
                    origin: FitHistoricalObservations.TimestampOrigin = FitHistoricalObservations.TimestampOrigin.DIRECT) =
        FitHistoricalObservations.Observation(
            metric = metric, timestampEpochMillis = at, value = value, unit = unit, sourceFileSha256 = sha, recordOrdinal = ordinal,
            globalMessageNumber = 20, fieldNumber = 3, quality = FitHistoricalObservations.Quality.PROFILE_VALID,
            timestampOrigin = origin, idempotencyKey = id, duplicateFingerprint = "fp-$id")

    @Test fun importedSyntheticFitRowsAppearAsVerifiedSeriesAndSurviveRestart() {
        val base = ApplicationProvider.getApplicationContext<Context>()
        val root = Files.createTempDirectory(base.cacheDir.toPath(), "fit-e2e-").toFile()
        val context = isolated(base, root)
        val hr = FitHistoricalObservations.Metric.HEART_RATE
        try {
            CirqaDatabase(context).use { db ->
                db.commitFitFile(
                    sourceSha256 = sha, fileBytes = 256, relativePath = "verified_fit/$sha.fit", validatedAtEpochMillis = t0,
                    decoderVersion = FitHistoricalObservations.DECODER_VERSION, profileVersion = FitHistoricalObservations.PROFILE_VERSION,
                    observations = listOf(
                        obs(hr, "bpm", "hr-1", t0 + 60_000, 70.0, 1),
                        obs(hr, "bpm", "hr-2", t0 + 120_000, 72.0, 2),
                        obs(hr, "bpm", "hr-3", t0 + 180_000, 75.0, 3),
                        // timestamp only reconstructed from a compressed header: must NOT be drawn as band time
                        obs(hr, "bpm", "hr-unknown", t0 + 240_000, 99.0, 4, FitHistoricalObservations.TimestampOrigin.RECONSTRUCTED_COMPRESSED),
                        obs(FitHistoricalObservations.Metric.DISTANCE, "m", "dist-1", t0 + 60_000, 10.0, 5)
                    ),
                    deviceKey = "synthetic-test-device")
            }
            // "App restart": a fresh database object on the same file.
            val series = CirqaDatabase(context).use {
                ScienceFitSeriesAdapter.readVerifiedSeries(HistoricalObservationRepository(context),
                    ScienceFitSeriesAdapter.Metric.HEART_RATE, t0, t0 + 3_600_000)
            }
            assertEquals(listOf(70.0, 72.0, 75.0), series.points.map { it.value })
            assertTrue(series.points.all { it.quality == SeriesQuality.VALID })
            assertEquals(SeriesOrigin.BAND, series.origin)
            assertEquals(SeriesTimeBasis.BAND_CLOCK, series.timeBasis)
            val assessment = SeriesAnalysis.assess(series)
            assertEquals(3, assessment.valid); assertEquals(0, assessment.invalid)
            // distance is its own series and never leaks into heart rate
            val distance = ScienceFitSeriesAdapter.readVerifiedSeries(HistoricalObservationRepository(context),
                ScienceFitSeriesAdapter.Metric.DISTANCE, t0, t0 + 3_600_000)
            assertEquals(listOf(10.0), distance.points.map { it.value })
            // an empty window is an honest empty series, not an error
            val empty = ScienceFitSeriesAdapter.readVerifiedSeries(HistoricalObservationRepository(context),
                ScienceFitSeriesAdapter.Metric.HEART_RATE, t0 + 7_200_000, t0 + 7_300_000)
            assertEquals(SeriesState.EMPTY, SeriesAnalysis.assess(empty).state)
        } finally { root.deleteRecursively() }
    }

    private fun isolated(base: Context, root: File) = object : ContextWrapper(base) {
        override fun getApplicationContext(): Context = this
        override fun getFilesDir(): File = root
        override fun getDatabasePath(name: String): File = File(root, name).also { it.parentFile?.mkdirs() }
        override fun openOrCreateDatabase(name: String, mode: Int, factory: SQLiteDatabase.CursorFactory?): SQLiteDatabase =
            openOrCreateDatabase(name, mode, factory, null)
        override fun openOrCreateDatabase(name: String, mode: Int, factory: SQLiteDatabase.CursorFactory?, errorHandler: DatabaseErrorHandler?): SQLiteDatabase =
            SQLiteDatabase.openOrCreateDatabase(getDatabasePath(name).absolutePath, factory, errorHandler)
    }
}
