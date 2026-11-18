package de.pulselibre

import android.content.Context
import android.content.ContextWrapper
import android.database.DatabaseErrorHandler
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteException
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import java.io.File
import java.nio.file.Files
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

/** Isolated synthetic rows in a temp directory; never touches saved health records (#62). */
@RunWith(AndroidJUnit4::class)
class DashboardDayReaderTest {
    private val fmt = DateTimeFormatter.ISO_INSTANT
    private fun pulse(at: Instant, bpm: Int) =
        """{"schema":1,"type":"reading","received_at_utc":"${fmt.format(at)}","service":6,"bpm":$bpm}"""

    private fun withContext(block: (Context) -> Unit) {
        val base = ApplicationProvider.getApplicationContext<Context>()
        val root = Files.createTempDirectory(base.cacheDir.toPath(), "day-reader-test-").toFile()
        try {
            block(object : ContextWrapper(base) {
                override fun getFilesDir(): File = root
                override fun getDatabasePath(name: String): File = File(root, name)
                override fun openOrCreateDatabase(name: String, mode: Int,
                    factory: SQLiteDatabase.CursorFactory?): SQLiteDatabase = openOrCreateDatabase(name, mode, factory, null)
                override fun openOrCreateDatabase(name: String, mode: Int,
                    factory: SQLiteDatabase.CursorFactory?, errorHandler: DatabaseErrorHandler?): SQLiteDatabase =
                    SQLiteDatabase.openOrCreateDatabase(getDatabasePath(name).absolutePath, factory, errorHandler)
            })
        } finally { root.deleteRecursively() }
    }

    @Test fun invalidValuesAndMalformedTimestampsNeverBecomePoints() = withContext { ctx ->
        val zone = ZoneId.of("UTC")
        val day = DashboardDay(LocalDate.of(2026, 11, 17), zone)
        val now = Instant.parse("2026-11-17T12:00:00Z")
        CirqaDatabase(ctx).use { db ->
            db.insertLine(pulse(Instant.parse("2026-11-17T08:00:00Z"), 70))
            db.insertLine(pulse(Instant.parse("2026-11-17T08:00:00Z"), 70)) // exact duplicate line
            db.insertLine(pulse(Instant.parse("2026-11-17T08:01:00Z"), 999)) // implausible
            db.insertLine("""{"schema":1,"type":"reading","received_at_utc":"not-a-time","service":6,"bpm":71}""")
            db.insertLine(pulse(Instant.parse("2026-11-17T13:00:00Z"), 72)) // future
        }
        val s = DashboardDayReader(ctx).read(DashboardMeasurement.PULSE, day, now)
        assertEquals(listOf(70L), s.points.map { it.value })
        assertEquals(1, s.count)
    }

    @Test fun manyReadingsAreBoundedPerMinuteButKeepExtremes() = withContext { ctx ->
        val day = DashboardDay(LocalDate.of(2026, 11, 17), ZoneId.of("UTC"))
        val start = Instant.parse("2026-11-17T00:00:00Z")
        CirqaDatabase(ctx).use { db ->
            for (i in 0 until 3000) db.insertLine(pulse(start.plusSeconds(i * 2L), 60 + (i % 50)))
            db.insertLine(pulse(start.plusSeconds(1), 200)) // spike must survive aggregation
        }
        val s = DashboardDayReader(ctx).read(DashboardMeasurement.PULSE, day, start.plusSeconds(86_399))
        assertEquals(3001, s.count)
        assertTrue(s.points.size <= s.minutesObserved * 3)
        assertTrue(s.points.any { it.value == 200L })
    }

    @Test fun sameInstantsFallIntoDifferentLocalDaysAfterZoneChange() = withContext { ctx ->
        val at = Instant.parse("2026-11-17T23:30:00Z")
        CirqaDatabase(ctx).use { it.insertLine(pulse(at, 66)) }
        val now = Instant.parse("2026-11-18T23:00:00Z")
        val berlin = DashboardDay(LocalDate.of(2026, 11, 17), ZoneId.of("Europe/Berlin"))
        val berlinNext = DashboardDay(LocalDate.of(2026, 11, 18), ZoneId.of("Europe/Berlin"))
        assertEquals(0, DashboardDayReader(ctx).read(DashboardMeasurement.PULSE, berlin, now).count)
        assertEquals(1, DashboardDayReader(ctx).read(DashboardMeasurement.PULSE, berlinNext, now).count)
    }

    @Test fun databaseNewerThanCodeIsRefusedWithoutLosingData() = withContext { ctx ->
        CirqaDatabase(ctx).use { it.insertLine(pulse(Instant.parse("2026-11-17T08:00:00Z"), 70)) }
        val file = ctx.getDatabasePath("cirqa_measurements.db")
        SQLiteDatabase.openDatabase(file.path, null, SQLiteDatabase.OPEN_READWRITE).use { it.version = 99 }
        try { CirqaDatabase(ctx).use { it.readableDatabase }; fail("downgrade must not be silent") }
        catch (_: SQLiteException) { /* expected: refuse instead of rewriting a newer schema */ }
        SQLiteDatabase.openDatabase(file.path, null, SQLiteDatabase.OPEN_READONLY).use { db ->
            db.rawQuery("SELECT COUNT(*) FROM events", null).use { c -> c.moveToFirst(); assertEquals(1, c.getInt(0)) }
        }
    }
}
