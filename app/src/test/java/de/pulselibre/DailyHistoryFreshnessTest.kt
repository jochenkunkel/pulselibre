package de.pulselibre

import org.junit.Assert.*
import org.junit.Test
import java.time.ZoneId
import java.util.Locale

/** #637 gap detection: how current the band's daily history is, said in words. */
class DailyHistoryFreshnessTest {
    private val zone = ZoneId.of("Europe/Berlin")
    private val now = 1_795_800_000_000L

    @Test fun noDailyHistoryIsStated() {
        assertTrue(DailyHistoryFreshness.text(GermanStrings, null, now, zone, Locale.GERMANY).startsWith("Noch keine Tageshistorie"))
    }

    @Test fun recentDataShowsTimeAndAgeWithoutGapHint() {
        val t = DailyHistoryFreshness.text(GermanStrings, now - 3 * 3_600_000L, now, zone, Locale.GERMANY)
        assertTrue(t, t.startsWith("Neueste Tageswerte vom Band:") && t.contains("vor 3 h"))
        assertFalse(t.contains("Mögliche Lücke"))
    }

    @Test fun olderThanOneDayAddsGapHint() {
        val t = DailyHistoryFreshness.text(GermanStrings, now - 30 * 3_600_000L, now, zone, Locale.GERMANY)
        assertTrue(t, t.contains("vor 30 h") && t.contains("Mögliche Lücke"))
    }

    @Test fun onlyDailyMetricsCountNotActivities() {
        assertEquals(listOf("stress", "spo2", "respiration"), DailyHistoryFreshness.DAILY_METRICS)
    }
}
