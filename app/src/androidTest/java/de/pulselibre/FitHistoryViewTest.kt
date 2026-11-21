package de.pulselibre

import android.content.Context
import android.content.ContextWrapper
import android.database.DatabaseErrorHandler
import android.database.sqlite.SQLiteDatabase
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.view.ContextThemeWrapper
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.nio.file.Files
import java.time.Instant

/** #334: FIT history appears in its own card with its own time basis and never mixes with live readings. */
@RunWith(AndroidJUnit4::class)
class FitHistoryViewTest {
    private fun observation(sha: String, id: String, timestamp: Long, value: Double, ordinal: Int) =
        FitHistoricalObservations.Observation(
            metric = FitHistoricalObservations.Metric.HEART_RATE, timestampEpochMillis = timestamp, value = value,
            unit = "bpm", sourceFileSha256 = sha, recordOrdinal = ordinal, globalMessageNumber = 20, fieldNumber = 3,
            quality = FitHistoricalObservations.Quality.PROFILE_VALID, idempotencyKey = id,
            duplicateFingerprint = "fingerprint-$id")

    private fun isolatedContext(base: Context, root: File) = object : ContextWrapper(base) {
        override fun getApplicationContext(): Context = this
        override fun getFilesDir(): File = root
        override fun getDatabasePath(name: String): File = File(root, name).also { it.parentFile?.mkdirs() }
        override fun openOrCreateDatabase(name: String, mode: Int, factory: SQLiteDatabase.CursorFactory?): SQLiteDatabase =
            openOrCreateDatabase(name, mode, factory, null)
        override fun openOrCreateDatabase(name: String, mode: Int, factory: SQLiteDatabase.CursorFactory?,
                                          errorHandler: DatabaseErrorHandler?): SQLiteDatabase =
            SQLiteDatabase.openOrCreateDatabase(getDatabasePath(name).absolutePath, factory, errorHandler)
    }

    private fun texts(view: View): List<String> =
        (if (view is TextView) listOf(view.text.toString()) else emptyList()) +
            if (view is ViewGroup) (0 until view.childCount).flatMap { texts(view.getChildAt(it)) } else emptyList()

    @Test fun readerReturnsOnlyWindowedFitPointsAndAvailability() {
        val base = AppLanguageSettings.wrap(ApplicationProvider.getApplicationContext<Context>())
        val root = Files.createTempDirectory(base.cacheDir.toPath(), "fit-history-reader-").toFile()
        val context = isolatedContext(base, root)
        val sha = "b".repeat(64)
        try {
            assertFalse(FitHistoryReader(context).read(Instant.ofEpochMilli(0), Instant.ofEpochMilli(10_000)).available)
            CirqaDatabase(context).use { db ->
                db.commitFitFile(
                    sourceSha256 = sha, fileBytes = 128, relativePath = "verified_fit/$sha.fit",
                    validatedAtEpochMillis = 10_000, decoderVersion = FitHistoricalObservations.DECODER_VERSION,
                    profileVersion = FitHistoricalObservations.PROFILE_VERSION,
                    observations = listOf(observation(sha, "a", 1_000, 70.0, 1), observation(sha, "b", 2_000, 72.0, 2),
                        observation(sha, "c", 9_000, 75.0, 3)),
                    deviceKey = "history-test-device")
            }
            val window = FitHistoryReader(context).read(Instant.ofEpochMilli(1_000), Instant.ofEpochMilli(3_000))
            assertTrue(window.available)
            assertFalse(window.truncated)
            assertEquals(listOf(1_000L, 2_000L), window.points.map { it.receivedAt.toEpochMilli() }) // half-open window
            assertEquals(listOf(70, 72), window.points.map { it.value })
            val reader = FitHistoryReader(context)
            assertEquals(Instant.ofEpochMilli(9_000), reader.latestMeasuredAt(Instant.ofEpochMilli(20_000)))
            assertEquals(Instant.ofEpochMilli(2_000), reader.latestMeasuredAt(Instant.ofEpochMilli(8_000)))
            assertEquals(null, reader.latestMeasuredAt(Instant.ofEpochMilli(500)))
        } finally { root.deleteRecursively() }
    }

    @Test fun fitCardIsSeparateAndNamesTheBandClock() {
        val context = ContextThemeWrapper(AppLanguageSettings.wrap(ApplicationProvider.getApplicationContext<Context>()), androidx.appcompat.R.style.Theme_AppCompat)
        val end = Instant.parse("2026-11-17T12:00:00Z")
        val live = listOf(HistoryPoint(end.minusSeconds(600), 80))
        val fit = listOf(HistoryPoint(end.minusSeconds(7_200), 61), HistoryPoint(end.minusSeconds(7_140), 63))
        lateinit var view: HistoryOverviewView
        InstrumentationRegistry.getInstrumentation().runOnMainSync {
            view = HistoryOverviewView(context) { range ->
                HistorySnapshot(range, range.startAt(end), end, live, emptyList(), fit, true)
            }
            view.refreshSynchronously()
            val all = texts(view)
            assertTrue(all.any { it == "Herzfrequenz · Band-Verlauf (FIT)" })
            assertTrue(all.any { it.contains("Zeitachse: Messzeit des Bandes") })
            assertTrue(all.any { it.contains("Zeitachse: Telefonzeit") })   // live card keeps its own clock
            assertTrue(all.any { it.startsWith("2 Werte") })                 // FIT card counts only FIT points
            assertTrue(all.any { it.startsWith("1 Werte") })                 // live card counts only live points
        }
    }

    @Test fun cardIsHiddenWithoutFitDataAndHonestWhenWindowIsEmpty() {
        val context = ContextThemeWrapper(AppLanguageSettings.wrap(ApplicationProvider.getApplicationContext<Context>()), androidx.appcompat.R.style.Theme_AppCompat)
        val end = Instant.parse("2026-11-17T12:00:00Z")
        InstrumentationRegistry.getInstrumentation().runOnMainSync {
            val none = HistoryOverviewView(context) { r -> HistorySnapshot(r, r.startAt(end), end, emptyList(), emptyList()) }
            none.refreshSynchronously()
            assertFalse(texts(none).any { it.contains("Band-Verlauf") })
            val empty = HistoryOverviewView(context) { r -> HistorySnapshot(r, r.startAt(end), end, emptyList(), emptyList(), emptyList(), true) }
            empty.refreshSynchronously()
            assertTrue(texts(empty).any { it == "Keine Band-Verlaufsdaten in diesem Zeitraum" })
        }
    }
}
