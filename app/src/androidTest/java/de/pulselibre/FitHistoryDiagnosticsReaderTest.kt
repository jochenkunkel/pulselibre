package de.pulselibre

import android.content.Context
import android.content.ContextWrapper
import android.database.sqlite.SQLiteDatabase
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import java.io.File
import java.nio.file.Files
import java.time.Instant
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class FitHistoryDiagnosticsReaderTest {
    @Test fun aggregatesOnlyActiveFitObservationsAndVisibleHeartRateWindowsReadOnly() {
        val base = ApplicationProvider.getApplicationContext<Context>()
        val root = Files.createTempDirectory(base.cacheDir.toPath(), "fit-history-diagnostic-").toFile()
        val context = isolatedContext(base, root)
        val now = Instant.parse("2026-11-22T12:00:00Z")
        try {
            SQLiteDatabase.openOrCreateDatabase(context.getDatabasePath("cirqa_measurements.db"), null).use { db ->
                db.execSQL("CREATE TABLE fit_active_generations(generation_id TEXT, source_sha256 TEXT)")
                db.execSQL("""CREATE TABLE fit_generation_observations(
                    generation_id TEXT, metric TEXT, unit TEXT, measured_at_ms INTEGER, value REAL)""")
                db.execSQL("INSERT INTO fit_active_generations VALUES('active-a','source-a')")
                fun row(generation: String, metric: String, unit: String, time: Instant, value: Double = 72.0) {
                    db.execSQL(
                        "INSERT INTO fit_generation_observations VALUES(?,?,?,?,?)",
                        arrayOf<Any?>(generation, metric, unit, time.toEpochMilli(), value)
                    )
                }
                row("active-a", "heart_rate", "bpm", now.minusSeconds(3600))
                row("active-a", "heart_rate", "bpm", now.minusSeconds(25 * 3600L))
                row("active-a", "heart_rate", "bpm", now.minusSeconds(8 * 86_400L))
                row("active-a", "heart_rate", "bpm", now.minusSeconds(31 * 86_400L))
                row("active-a", "heart_rate", "bpm", now.minusSeconds(120), 250.0)
                row("active-a", "steps", "steps", now.minusSeconds(120))
                row("inactive-b", "heart_rate", "bpm", now.minusSeconds(60))
            }
            val dbFile = context.getDatabasePath("cirqa_measurements.db")
            val beforeBytes = dbFile.length()
            val beforeVersion = SQLiteDatabase.openDatabase(
                dbFile.absolutePath, null, SQLiteDatabase.OPEN_READONLY
            ).use { it.version }

            val result = FitHistoryDiagnosticsReader.read(context, now)

            assertTrue(result.available)
            assertEquals(1L, result.sourceCount)
            val hr = result.heartRate!!
            assertEquals(4L, hr.total)
            assertEquals(1L, hr.last24Hours)
            assertEquals(2L, hr.last7Days)
            assertEquals(3L, hr.last30Days)
            assertEquals(now.minusSeconds(31 * 86_400L), hr.firstMeasuredAt)
            assertEquals(now.minusSeconds(3600), hr.lastMeasuredAt)
            assertEquals(5L, result.metrics.single { it.metric == "heart_rate" }.count)
            assertEquals(1L, result.metrics.single { it.metric == "steps" }.count)
            assertEquals(beforeBytes, dbFile.length())
            val afterVersion = SQLiteDatabase.openDatabase(
                dbFile.absolutePath, null, SQLiteDatabase.OPEN_READONLY
            ).use { it.version }
            assertEquals(beforeVersion, afterVersion)
        } finally {
            root.deleteRecursively()
        }
    }

    @Test fun readsLegacyFitProvenanceWhenGenerationSchemaIsAbsent() {
        val base = ApplicationProvider.getApplicationContext<Context>()
        val root = Files.createTempDirectory(base.cacheDir.toPath(), "fit-history-legacy-diagnostic-").toFile()
        val context = isolatedContext(base, root)
        val now = Instant.parse("2026-11-22T12:00:00Z")
        try {
            SQLiteDatabase.openOrCreateDatabase(context.getDatabasePath("cirqa_measurements.db"), null).use { db ->
                db.execSQL("""CREATE TABLE observations(
                    sample_id TEXT, device_id TEXT, metric TEXT, measured_at_ms INTEGER, unit TEXT, value REAL)""")
                db.execSQL("""CREATE TABLE fit_observation_provenance(
                    sample_id TEXT, device_id TEXT, metric TEXT, measured_at_ms INTEGER)""")
                db.execSQL(
                    "INSERT INTO observations VALUES('sample-1','fit-device','heart_rate',?,'bpm',?)",
                    arrayOf<Any?>(now.minusSeconds(600).toEpochMilli(), 74.0)
                )
                db.execSQL(
                    "INSERT INTO fit_observation_provenance VALUES('sample-1','fit-device','heart_rate',?)",
                    arrayOf(now.minusSeconds(600).toEpochMilli())
                )
            }
            val result = FitHistoryDiagnosticsReader.read(context, now)
            assertTrue(result.available)
            assertNull(result.sourceCount)
            assertEquals(1L, result.heartRate?.total)
            assertEquals(1L, result.heartRate?.last24Hours)
            assertEquals(now.minusSeconds(600), result.heartRate?.lastMeasuredAt)
        } finally {
            root.deleteRecursively()
        }
    }

    @Test fun absentDatabaseIsReportedUnavailableWithoutCreatingIt() {
        val base = ApplicationProvider.getApplicationContext<Context>()
        val root = Files.createTempDirectory(base.cacheDir.toPath(), "fit-history-missing-diagnostic-").toFile()
        val context = isolatedContext(base, root)
        try {
            val result = FitHistoryDiagnosticsReader.read(context, Instant.parse("2026-11-22T12:00:00Z"))
            assertFalse(result.available)
            assertNull(result.heartRate)
            assertFalse(context.getDatabasePath("cirqa_measurements.db").exists())
        } finally {
            root.deleteRecursively()
        }
    }

    private fun isolatedContext(base: Context, root: File) = object : ContextWrapper(base) {
        override fun getFilesDir(): File = root
        override fun getDatabasePath(name: String): File = File(root, name)
        override fun openOrCreateDatabase(name: String, mode: Int,
            factory: SQLiteDatabase.CursorFactory?): SQLiteDatabase =
            SQLiteDatabase.openOrCreateDatabase(getDatabasePath(name).absolutePath, factory)
    }
}
