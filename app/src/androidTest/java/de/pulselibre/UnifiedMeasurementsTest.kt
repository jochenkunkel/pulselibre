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
            unit TEXT NOT NULL, received_at_ms INTEGER, global_message_number INTEGER NOT NULL DEFAULT 55,
            timestamp_origin TEXT NOT NULL DEFAULT 'DIRECT', PRIMARY KEY(generation_id, sample_id))""")
        db.execSQL("CREATE TABLE fit_active_generations(source_sha256 TEXT PRIMARY KEY NOT NULL, generation_id TEXT NOT NULL UNIQUE)")
        db.execSQL("CREATE TABLE fit_decoder_generations(generation_id TEXT PRIMARY KEY NOT NULL, output_count INTEGER NOT NULL)")
    }

    @After fun close() = db.close()

    private fun live(bpm: Int, ms: Long) = db.execSQL("INSERT INTO events(kind, payload_json) VALUES('reading', ?)",
        arrayOf("""{"schema":1,"type":"reading","service":6,"received_at_utc":"${java.time.Instant.ofEpochMilli(ms)}","bpm":$bpm}"""))

    private fun band(id: String, value: Double, ms: Long, generation: String = "g1", message: Int = 55,
                     origin: String = "DIRECT", device: String = "fit", unit: String = "bpm") {
        db.execSQL("INSERT OR IGNORE INTO fit_decoder_generations VALUES(?, 0)", arrayOf(generation))
        // One active generation per source file; the file of generation gN is "sN".
        db.execSQL("INSERT OR IGNORE INTO fit_active_generations VALUES(?, ?)", arrayOf("s" + generation.drop(1), generation))
        db.execSQL("""INSERT INTO fit_generation_observations(generation_id, sample_id, device_id, metric, measured_at_ms, value,
            unit, received_at_ms, global_message_number, timestamp_origin) VALUES(?, ?, ?, 'heart_rate', ?, ?, ?, NULL, ?, ?)""",
            arrayOf<Any>(generation, id, device, ms, value, unit, message, origin))
    }

    @Test fun sameMeasurementInTwoFilesCountsOnceButStaysExported() {
        // R1 (FIT, 25.11.2026): same device, metric, valid time, unit and value in two different files.
        band("a1", 60.0, t0 + 1000, "g1"); band("b1", 60.0, t0 + 1000, "g2")
        // Not duplicates: other value, other unit, other device, unknown time base, or twice inside one file.
        band("a2", 61.0, t0 + 2000, "g1"); band("b2", 62.0, t0 + 2000, "g2")
        band("a3", 63.0, t0 + 3000, "g1"); band("b3", 63.0, t0 + 3000, "g2", unit = "count")
        band("a4", 64.0, t0 + 4000, "g1"); band("b4", 64.0, t0 + 4000, "g2", device = "other")
        band("a5", 65.0, t0 + 5000, "g1", origin = "UNKNOWN"); band("b5", 65.0, t0 + 5000, "g2", origin = "UNKNOWN")
        band("a6", 66.0, t0 + 6000, "g1"); band("a7", 66.0, t0 + 6000, "g1")
        val unified = UnifiedMeasurements(db)
        unified.refresh()
        val shown = unified.points(MeasurementOrigin.BAND, "heart_rate", t0, t0 + day)
        assertEquals(11, shown.size)
        assertEquals(1, shown.count { it.first == t0 + 1000 })
        assertEquals(1, unified.bandDuplicateCount("heart_rate", t0, t0 + day))
        assertEquals(11, unified.bandKindCounts("heart_rate", t0, t0 + day)[BandRecordKind.MONITORING])
        assertEquals(10, unified.dailySummary(t0, t0 + day, 0).single { it.unit == "bpm" }.count)
        // The export keeps every original row, the duplicate included.
        assertEquals(12, unified.records(t0, t0 + day).size)
    }

    @Test fun normalHistoryAndActivityStayDistinguishable() {
        band("m1", 60.0, t0 + 1000, message = 55); band("m2", 61.0, t0 + 2000, message = 55)
        band("a1", 140.0, t0 + 3000, message = 20); band("x1", 70.0, t0 + 4000, message = 999)
        val unified = UnifiedMeasurements(db)
        unified.refresh()
        val kinds = unified.bandKindCounts("heart_rate", t0, t0 + day)
        assertEquals(2, kinds[BandRecordKind.MONITORING]); assertEquals(1, kinds[BandRecordKind.ACTIVITY])
        assertEquals(1, kinds[BandRecordKind.OTHER])
        // An activity alone is no normal history: nothing of it may be counted as monitoring.
        assertEquals(mapOf(BandRecordKind.ACTIVITY to 1), unified.bandKindCounts("heart_rate", t0 + 2500, t0 + 3500))
    }

    @Test fun versionOneTableIsUpgradedInPlaceWithoutReReadingLiveEvents() {
        // A copy from before `fit_message`, with its live cursor already at the newest event.
        live(80, t0 + 500)
        db.execSQL("""CREATE TABLE unified_measurements(origin TEXT NOT NULL, device_id TEXT NOT NULL, metric TEXT NOT NULL,
            measured_at_ms INTEGER NOT NULL, value REAL NOT NULL, unit TEXT NOT NULL, sample_id TEXT NOT NULL DEFAULT '',
            received_at_ms INTEGER, PRIMARY KEY(origin, device_id, metric, measured_at_ms, sample_id)) WITHOUT ROWID""")
        db.execSQL("INSERT INTO unified_measurements VALUES('live', 'CIRQA', 'bpm', ?, 77, 'bpm', 'kept', NULL)", arrayOf<Any>(t0 + 10))
        db.execSQL("INSERT INTO unified_measurements VALUES('band', 'fit', 'heart_rate', ?, 99, 'bpm', 'stale', NULL)", arrayOf<Any>(t0 + 20))
        db.execSQL("CREATE TABLE unified_state(key TEXT PRIMARY KEY NOT NULL, value TEXT NOT NULL)")
        db.execSQL("INSERT INTO unified_state VALUES('live_event_id', '1')")
        db.execSQL("INSERT INTO unified_state VALUES('historical_signature', 'old')")
        band("m1", 60.0, t0 + 1000, message = 55)
        val unified = UnifiedMeasurements(db)
        unified.refresh()
        // Live rows stay as they were (no full re-read of all events); event 1 is not parsed again.
        assertEquals(listOf(77.0), unified.points(MeasurementOrigin.LIVE, "bpm", t0, t0 + day).map { it.second })
        // Band rows are rebuilt with their kind; the stale band row is gone.
        assertEquals(listOf(60.0), unified.points(MeasurementOrigin.BAND, "heart_rate", t0, t0 + day).map { it.second })
        assertEquals(mapOf(BandRecordKind.MONITORING to 1), unified.bandKindCounts("heart_rate", t0, t0 + day))
    }

    @Test fun tableOfBuild2171IsRebuiltOnceCompletely() {
        // Build 2171 created the new layout but no schema marker, and could lose live rows: rebuild once.
        live(80, t0 + 1000); live(81, t0 + 2000)
        UnifiedMeasurements(db).ensureSchema()
        db.execSQL("DELETE FROM unified_state")                       // as left by 2171: no schema marker
        db.execSQL("INSERT INTO unified_state VALUES('live_event_id', '2')")   // cursor claims all events are done
        val unified = UnifiedMeasurements(db)
        unified.refresh()
        assertEquals(listOf(80.0, 81.0), unified.points(MeasurementOrigin.LIVE, "bpm", t0, t0 + day).map { it.second })
        // Afterwards the marker exists and nothing is rebuilt again.
        live(82, t0 + 3000)
        unified.refresh()
        assertEquals(3, unified.points(MeasurementOrigin.LIVE, "bpm", t0, t0 + day).size)
    }

    @Test fun parallelRefreshesFromSeveralThreadsLoseNothing() {
        for (i in 0 until 3000) live(60 + i % 40, t0 + i * 1000L)
        val path = java.io.File.createTempFile("unified", ".db").apply { delete() }
        val file = SQLiteDatabase.openOrCreateDatabase(path, null)
        try {
            // Same schema in a real file so several connections can work on it at once.
            for (sql in listOf("CREATE TABLE events(id INTEGER PRIMARY KEY AUTOINCREMENT, kind TEXT NOT NULL, payload_json TEXT NOT NULL)",
                    "CREATE TABLE observations(device_id TEXT NOT NULL, metric TEXT NOT NULL, measured_at_ms INTEGER NOT NULL, value REAL NOT NULL, unit TEXT NOT NULL, sample_id TEXT NOT NULL DEFAULT '', received_at_ms INTEGER, PRIMARY KEY(device_id, metric, measured_at_ms, sample_id))",
                    "CREATE TABLE fit_observation_provenance(sample_id TEXT PRIMARY KEY NOT NULL)",
                    "CREATE TABLE fit_generation_observations(generation_id TEXT NOT NULL, sample_id TEXT NOT NULL, device_id TEXT NOT NULL, metric TEXT NOT NULL, measured_at_ms INTEGER NOT NULL, value REAL NOT NULL, unit TEXT NOT NULL, received_at_ms INTEGER, global_message_number INTEGER NOT NULL DEFAULT 55, PRIMARY KEY(generation_id, sample_id))",
                    "CREATE TABLE fit_active_generations(generation_id TEXT PRIMARY KEY NOT NULL)",
                    "CREATE TABLE fit_decoder_generations(generation_id TEXT PRIMARY KEY NOT NULL, output_count INTEGER NOT NULL)")) file.execSQL(sql)
        } finally { file.close() }
        val copy = SQLiteDatabase.openOrCreateDatabase(path, null)
        copy.beginTransaction()
        try {
            db.rawQuery("SELECT kind, payload_json FROM events ORDER BY id", null).use { c ->
                while (c.moveToNext()) copy.execSQL("INSERT INTO events(kind, payload_json) VALUES(?, ?)", arrayOf(c.getString(0), c.getString(1)))
            }
            copy.setTransactionSuccessful()
        } finally { copy.endTransaction(); copy.close() }
        val errors = java.util.concurrent.ConcurrentLinkedQueue<Throwable>()
        val threads = (1..4).map {
            Thread {
                try { SQLiteDatabase.openDatabase(path.path, null, SQLiteDatabase.OPEN_READWRITE).use { UnifiedMeasurements(it).refresh() } }
                catch (t: Throwable) { errors.add(t) }
            }
        }
        threads.forEach { it.start() }; threads.forEach { it.join(60_000) }
        assertTrue(errors.toString(), errors.isEmpty())
        SQLiteDatabase.openDatabase(path.path, null, SQLiteDatabase.OPEN_READWRITE).use {
            assertEquals(3000, UnifiedMeasurements(it).points(MeasurementOrigin.LIVE, "bpm", t0, t0 + day).size)
        }
        SQLiteDatabase.deleteDatabase(path)
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

    @Test fun readersServeBandPointsExportRecordsAndUnreadableEvents() {
        live(80, t0 + 1000)
        db.execSQL("INSERT INTO events(kind, payload_json) VALUES('reading', 'kaputt')")
        db.execSQL("INSERT INTO events(kind, payload_json) VALUES('reading', ?)",
            arrayOf("""{"schema":1,"type":"reading","service":6,"received_at_utc":"gestern","bpm":70}"""))
        band("b1", 61.0, t0 + 2000); band("b2", 62.0, t0 + 3000)
        val unified = UnifiedMeasurements(db)
        unified.refresh()
        assertEquals(2L, unified.unreadableLiveEvents())
        assertEquals(listOf(61.0, 62.0), unified.points(MeasurementOrigin.BAND, "heart_rate", t0, t0 + day).map { it.second })
        assertEquals(1, unified.points(MeasurementOrigin.BAND, "heart_rate", t0, t0 + day, limit = 1).size)
        assertEquals(t0 + 3000, unified.latest(MeasurementOrigin.BAND, "heart_rate", Long.MAX_VALUE / 4))
        assertTrue(unified.exists(MeasurementOrigin.BAND, "heart_rate"))
        assertEquals(3, unified.records(t0, t0 + day).size)
        assertEquals(0, unified.records(t0 + day, t0 + 2 * day).size)
    }

    private fun liveRaw(service: Int, field: String, value: Int, ms: Long) = db.execSQL("INSERT INTO events(kind, payload_json) VALUES('reading', ?)",
        arrayOf("""{"schema":1,"type":"reading","service":$service,"received_at_utc":"${java.time.Instant.ofEpochMilli(ms)}","$field":$value}"""))

    @Test fun statusCodesAreCountedAsInvalidAndNeverAsMeasurements() {
        liveRaw(21, "breaths_per_min", 14, t0 + 1000); liveRaw(21, "breaths_per_min", -100, t0 + 2000)
        liveRaw(21, "breaths_per_min", -200, t0 + 3000); liveRaw(21, "breaths_per_min", -300, t0 + 4000)
        liveRaw(6, "bpm", 0, t0 + 5000); liveRaw(6, "bpm", 70, t0 + 6000)
        liveRaw(13, "stress", -1, t0 + 7000); liveRaw(13, "stress", 30, t0 + 8000)
        val unified = UnifiedMeasurements(db)
        unified.refresh()
        val rows = unified.dailySummary(t0, t0 + day, 0).associateBy { it.metric }
        val breath = rows.getValue("breaths_per_min")
        assertEquals(1, breath.count); assertEquals(3, breath.invalid)
        assertEquals(14.0, breath.minimum, 0.0); assertEquals(14.0, breath.maximum, 0.0)
        val pulse = rows.getValue("bpm")
        assertEquals(1, pulse.count); assertEquals(1, pulse.invalid); assertEquals(70.0, pulse.minimum, 0.0)
        val stress = rows.getValue("stress")
        assertEquals(1, stress.count); assertEquals(1, stress.invalid); assertEquals(30.0, stress.mean, 0.0)
    }

    @Test fun aRowWithOnlyInvalidValuesHasNoValidCount() {
        liveRaw(21, "breaths_per_min", -100, t0 + 1000)
        val unified = UnifiedMeasurements(db)
        unified.refresh()
        val row = unified.dailySummary(t0, t0 + day, 0).single()
        assertEquals(0, row.count); assertEquals(1, row.invalid)
    }
}

/** Real-series loading for the dashboard: bucketed valid values, invalid/status flagged, origins kept apart. */
@RunWith(AndroidJUnit4::class)
class ScienceSeriesLoaderTest {
    private lateinit var db: SQLiteDatabase
    private val t0 = 1_795_000_000_000L - 1_795_000_000_000L % 86_400_000L

    @Before fun open() {
        db = SQLiteDatabase.create(null)
        UnifiedMeasurements(db).ensureSchema()
    }

    @After fun close() = db.close()

    private fun put(origin: String, metric: String, unit: String, ms: Long, value: Double, id: String) =
        db.execSQL("INSERT INTO unified_measurements VALUES(?, 'd', ?, ?, ?, ?, ?, NULL, NULL, 0)", arrayOf<Any>(origin, metric, ms, value, unit, id))

    @Test fun heartRateIsBucketedAndZeroIsInvalidNotAValue() {
        put("band", "heart_rate", "bpm", t0 + 1_000, 60.0, "a"); put("band", "heart_rate", "bpm", t0 + 2_000, 80.0, "b")
        put("band", "heart_rate", "bpm", t0 + 3_000, 0.0, "c")
        put("live", "bpm", "bpm", t0 + 4_000, 99.0, "d") // other origin, must not leak in
        val data = ScienceSeriesLoader.load(db, ScienceSeriesLoader.HEART_RATE_BAND, t0, t0 + 86_400_000L)
        val a = SeriesAnalysis.assess(data)
        assertEquals(2, a.valid); assertEquals(1, a.invalid)
        assertEquals(60.0, a.minimum!!, 0.0); assertEquals(80.0, a.maximum!!, 0.0); assertEquals(70.0, a.mean!!, 0.0)
        assertEquals(SeriesOrigin.BAND, data.origin); assertEquals(SeriesTimeBasis.BAND_CLOCK, data.timeBasis)
    }

    @Test fun respirationStatusCodesAreStatusAndOtherNegativesInvalid() {
        put("live", "breaths_per_min", "breaths/min", t0 + 1_000, 14.0, "a")
        put("live", "breaths_per_min", "breaths/min", t0 + 2_000, -100.0, "b"); put("live", "breaths_per_min", "breaths/min", t0 + 3_000, -200.0, "c")
        put("live", "breaths_per_min", "breaths/min", t0 + 4_000, -300.0, "d"); put("live", "breaths_per_min", "breaths/min", t0 + 5_000, -7.0, "e")
        val a = SeriesAnalysis.assess(ScienceSeriesLoader.load(db, ScienceSeriesLoader.RESPIRATION_LIVE, t0, t0 + 86_400_000L))
        assertEquals(1, a.valid); assertEquals(3, a.status); assertEquals(1, a.invalid)
        assertEquals(14.0, a.minimum!!, 0.0)
    }

    @Test fun spo2StaysUnverifiedAndStressNegativeIsInvalid() {
        put("live", "spo2_percent", "%", t0 + 1_000, 97.0, "a"); put("live", "stress", "device_scale", t0 + 1_000, 30.0, "b")
        put("live", "stress", "device_scale", t0 + 2_000, -1.0, "c")
        val spo2 = SeriesAnalysis.assess(ScienceSeriesLoader.load(db, ScienceSeriesLoader.SPO2_LIVE, t0, t0 + 86_400_000L))
        assertEquals(1, spo2.unverified); assertEquals(0, spo2.valid)
        val stress = SeriesAnalysis.assess(ScienceSeriesLoader.load(db, ScienceSeriesLoader.STRESS_LIVE, t0, t0 + 86_400_000L))
        assertEquals(1, stress.valid); assertEquals(1, stress.invalid)
    }

    @Test fun emptyTableGivesAnEmptySeriesNotZeros() {
        assertEquals(SeriesState.EMPTY, SeriesAnalysis.assess(ScienceSeriesLoader.load(db, ScienceSeriesLoader.HEART_RATE_LIVE, t0, t0 + 86_400_000L)).state)
    }
}
