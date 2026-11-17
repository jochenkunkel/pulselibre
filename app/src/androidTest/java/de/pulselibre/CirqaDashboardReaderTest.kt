package de.pulselibre

import android.content.Context
import android.content.ContextWrapper
import android.database.DatabaseErrorHandler
import android.database.sqlite.SQLiteDatabase
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import java.io.File
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.nio.file.Files
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

/** Uses isolated synthetic SQLite rows; never reads the app's saved health records. */
@RunWith(AndroidJUnit4::class)
class CirqaDashboardReaderTest {
    @Test fun readsReceptionMetadataAndEstimatesModeFromCumulativeStepSnapshots() {
        val base = ApplicationProvider.getApplicationContext<Context>()
        val root = Files.createTempDirectory(base.cacheDir.toPath(), "dashboard-reader-test-").toFile()
        val context = isolatedContext(base, root)
        val now = Instant.parse("2026-11-17T12:00:00Z")
        val fmt = DateTimeFormatter.ISO_INSTANT
        fun steps(secondsAgo: Long, count: Long) = """{"schema":1,"type":"reading","received_at_utc":"${fmt.format(now.minusSeconds(secondsAgo))}","service":7,"steps":$count,"goal":10000}"""
        val pulse = """{"schema":1,"type":"reading","received_at_utc":"${fmt.format(now.minusSeconds(30))}","service":6,"bpm":71}"""
        try {
            CirqaDatabase(context).use { db ->
                listOf(steps(300, 1_000), steps(180, 1_200), steps(60, 1_400), steps(0, 1_505), pulse)
                    .forEach { assertTrue(db.insertLine(it)) }
            }
            val summary = CirqaDashboardReader(context).read(now)
            assertEquals(PulseContext.SPORT, summary.activity.context)
            assertEquals(101.0, summary.activity.cadenceStepsPerMinute!!, 0.001)
            val pulseMetric = summary.metrics.single { it.title == "Herzfrequenz" }
            assertEquals(now.minusSeconds(30), pulseMetric.receivedAt)
            assertEquals("71 bpm", pulseMetric.value)
            val stepsMetric = summary.metrics.single { it.title == "Schritte (Tageszähler)" }
            assertEquals(now, stepsMetric.receivedAt)
            assertEquals("1505", stepsMetric.value)
        } finally {
            root.deleteRecursively()
        }
    }

    private fun isolatedContext(base: Context, root: File) = object : ContextWrapper(base) {
        override fun getFilesDir(): File = root
        override fun getDatabasePath(name: String): File = File(root, name)
        override fun openOrCreateDatabase(name: String, mode: Int,
            factory: SQLiteDatabase.CursorFactory?): SQLiteDatabase = openOrCreateDatabase(name, mode, factory, null)
        override fun openOrCreateDatabase(name: String, mode: Int,
            factory: SQLiteDatabase.CursorFactory?, errorHandler: DatabaseErrorHandler?): SQLiteDatabase =
            SQLiteDatabase.openOrCreateDatabase(getDatabasePath(name).absolutePath, factory, errorHandler)
    }
}
