package de.pulselibre

import android.database.sqlite.SQLiteDatabase
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/** One table for live, imported and band data: incremental, idempotent, origins stay apart. */
@RunWith(AndroidJUnit4::class)
class UnifiedMeasurementsTest {
    private lateinit var db: SQLiteDatabase
    private val day = 86_400_000L
    private val t0 = 1_795_000_000_000L - 1_795_000_000_000L % day // midnight UTC of some day

    @Before fun open() {
        db = SQLiteDatabase.create(null)
        db.execSQL("CREATE TABLE events(id INTEGER PRIMARY KEY AUTOINCREMENT, kind TEXT NOT NULL, payload_json TEXT NOT NULL)")
        db.execSQL("""CREATE TABLE observations(device_id TEXT NOT NULL, metric TEXT NOT NULL, measured_at_ms INTEGER NOT NULL,
            value REAL NOT NULL, unit TEXT NOT NULL, sample_id TEXT NOT NULL DEFAULT '', received_at_ms INTEGER,
            PRIMARY KEY(device_id, metric, measured_at_ms, sample_id))""")
        db.execSQL("CREATE TABLE fit_observation_provenance(sample_id TEXT PRIMARY KEY NOT NULL)")
        db.execSQL("""CREATE TABLE fit_generation_observations(generation_id TEXT NOT NULL, sample_id TEXT NOT NULL,
            device_id TEXT NOT NULL, metric TEXT NOT NULL, measured_at_ms INTEGER NOT NULL, value REAL NOT NULL,
            unit TEXT NOT NULL, received_at_ms INTEGER, PRIMARY KEY(generation_id, sample_id))""")
        db.execSQL("CREATE TABLE fit_active_generations(generation_id TEXT PRIMARY KEY NOT NULL)")
        db.execSQL("CREATE TABLE fit_decoder_generations(generation_id TEXT PRIMARY KEY NOT NULL, output_count INTEGER NOT NULL)")
    }

    @After fun close() = db.close()

    private fun live(bpm: Int, ms: Long) = db.execSQL("INSERT INTO events(kind, payload_json) VALUES('reading', ?)",
        arrayOf("""{"schema":1,"type":"reading","service":6,"received_at_utc":"${java.time.Instant.ofEpochMilli(ms)}","bpm":$bpm}"""))

    private fun band(id: String, value: Double, ms: Long, generation: String = "g1") {
        db.execSQL("INSERT OR IGNORE INTO fit_decoder_generations VALUES(?, 0)", arrayOf(generation))
        db.execSQL("INSERT OR IGNORE INTO fit_active_generations VALUES(?)", arrayOf(generation))
        db.execSQL("INSERT INTO fit_generation_observations VALUES(?, ?, 'fit', 'heart_rate', ?, ?, 'bpm', NULL)",
            arrayOf<Any>(generation, id, ms, value))
    }

    @Test fun liveBandAndImportSitInOneTableWithSeparateOrigins() {
        live(80, t0 + 1000); live(90, t0 + 2000)
        band("s1", 60.0, t0 + 3000); band("s2", 100.0, t0 + 4000)
        db.execSQL("INSERT INTO observations VALUES('old', 'steps', ?, 5, 'steps', 'a', NULL)", arrayOf<Any>(t0 + 5000))
        val unified = UnifiedMeasurements(db)
        unified.refresh()
        val rows = unified.dailySummary(t0, t0 + day, 0)
        assertEquals(setOf(MeasurementOrigin.LIVE, MeasurementOrigin.BAND, MeasurementOrigin.IMPORT), rows.map { it.origin }.toSet())
        val liveRow = rows.single { it.origin == MeasurementOrigin.LIVE }
        assertEquals(2, liveRow.count); assertEquals(80.0, liveRow.minimum, 0.0); assertEquals(85.0, liveRow.mean, 0.0)
        val bandRow = rows.single { it.origin == MeasurementOrigin.BAND }
        assertEquals("heart_rate", bandRow.metric); assertEquals(100.0, bandRow.maximum, 0.0)
    }

    @Test fun refreshIsIdempotentAndIncremental() {
        live(80, t0 + 1000)
        val unified = UnifiedMeasurements(db)
        unified.refresh(); unified.refresh()
        assertEquals(1, unified.dailySummary(t0, t0 + day, 0).single().count)
        live(82, t0 + 2000)
        unified.refresh()
        assertEquals(2, unified.dailySummary(t0, t0 + day, 0).single().count)
    }

    @Test fun replacedEventsRebuildTheLiveOrigin() {
        live(80, t0 + 1000); live(81, t0 + 2000)
        val unified = UnifiedMeasurements(db)
        unified.refresh()
        db.execSQL("DELETE FROM events"); db.execSQL("DELETE FROM sqlite_sequence WHERE name = 'events'")
        live(70, t0 + 3000)
        unified.refresh()
        val row = unified.dailySummary(t0, t0 + day, 0).single()
        assertEquals(1, row.count); assertEquals(70.0, row.minimum, 0.0)
    }

    @Test fun changedBandGenerationReplacesItsRows() {
        band("s1", 60.0, t0 + 1000)
        val unified = UnifiedMeasurements(db)
        unified.refresh()
        db.execSQL("DELETE FROM fit_generation_observations"); db.execSQL("DELETE FROM fit_active_generations")
        band("n1", 70.0, t0 + 2000, "g2"); band("n2", 72.0, t0 + 3000, "g2")
        unified.refresh()
        val row = unified.dailySummary(t0, t0 + day, 0).single()
        assertEquals(2, row.count); assertEquals(70.0, row.minimum, 0.0)
    }

    @Test fun daysFollowTheGivenOffsetAndNewestComesFirst() {
        live(80, t0 + day - 1000) // 23:59:59 UTC
        live(81, t0 + 2 * day + 1000)
        val unified = UnifiedMeasurements(db)
        unified.refresh()
        val rows = unified.dailySummary(t0, t0 + 3 * day, 3_600_000L)
        assertTrue(rows[0].dayIndex > rows[1].dayIndex)
        assertEquals(t0 / day + 1, rows[1].dayIndex) // +1 h pushes 23:59:59 UTC to the next local day
    }
}
