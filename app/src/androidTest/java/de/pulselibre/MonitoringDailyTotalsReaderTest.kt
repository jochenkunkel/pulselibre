package de.pulselibre

import android.database.sqlite.SQLiteDatabase
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

/** #329/#778: counters and activity types carry different record ordinals; they are paired per timestamp by rank. */
@RunWith(AndroidJUnit4::class)
class MonitoringDailyTotalsReaderTest {
    private fun db() = SQLiteDatabase.create(null).apply {
        execSQL("CREATE TABLE fit_active_generations (generation_id TEXT)")
        execSQL("""CREATE TABLE fit_generation_observations (generation_id TEXT, measured_at_ms INTEGER, record_ordinal INTEGER,
            metric TEXT, value REAL, global_message_number INTEGER, field_number INTEGER)""")
        execSQL("INSERT INTO fit_active_generations VALUES ('g1')")
    }
    private fun SQLiteDatabase.obs(gen: String, t: Long, ord: Int, metric: String, v: Double, field: Int) =
        execSQL("INSERT INTO fit_generation_observations VALUES (?,?,?,?,?,55,?)", arrayOf<Any>(gen, t, ord, metric, v, field))

    @Test fun pairsByTimestampAndRankAndSkipsInactiveOrAmbiguousRecords() {
        val d = db()
        // two records at t=1000: generic (0) 717 kcal, walking (6) 761 kcal; ordinals of the two paths differ
        d.obs("g1", 1000, 2595, "active_calories", 717.0, 19); d.obs("g1", 1000, 2596, "active_calories", 761.0, 19)
        d.obs("g1", 1000, 631, "monitoring_activity_type", 0.0, 5); d.obs("g1", 1000, 632, "monitoring_activity_type", 6.0, 5)
        // type from field 24 for a later record
        d.obs("g1", 2000, 2700, "active_calories", 770.0, 19); d.obs("g1", 2000, 700, "monitoring_current_activity_type", 6.0, 24)
        // ambiguous: two counters, one type -> skipped
        d.obs("g1", 3000, 2800, "active_calories", 1.0, 19); d.obs("g1", 3000, 2801, "active_calories", 2.0, 19)
        d.obs("g1", 3000, 800, "monitoring_activity_type", 6.0, 5)
        // inactive generation -> ignored
        d.obs("g0", 1000, 1, "active_calories", 999.0, 19); d.obs("g0", 1000, 1, "monitoring_activity_type", 6.0, 5)
        val s = MonitoringDailyTotals.readActiveCalories(d, 0, 10_000).sortedWith(compareBy({ it.timeMs }, { it.value }))
        assertEquals(listOf(Triple(1000L, 0, 717.0), Triple(1000L, 6, 761.0), Triple(2000L, 6, 770.0)),
            s.map { Triple(it.timeMs, it.activityType, it.value) })
        d.close()
    }
}
