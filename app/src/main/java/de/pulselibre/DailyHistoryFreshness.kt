package de.pulselibre

import android.content.Context
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

/**
 * #637 gap detection: how current is the band's DAILY history (all-day FIT monitoring metrics), independent of the last
 * download report. Activity recordings do not count – they say nothing about stress/SpO2/breathing over the day.
 */
internal object DailyHistoryFreshness {
    /** Metrics that only come from daily-history files (monitoring_b, FIT 227/269/297). */
    val DAILY_METRICS = listOf("stress", "spo2", "respiration")
    const val GAP_HOURS = 24L

    /** Newest band time of any daily metric, or null when none is stored. */
    fun latest(context: Context, now: Long = System.currentTimeMillis()): Long? = try {
        CirqaDatabase(context).use { db ->
            val unified = UnifiedMeasurements(db.writableDatabase)
            unified.refresh()
            DAILY_METRICS.mapNotNull { unified.latest(MeasurementOrigin.BAND, it, now) }.maxOrNull()
        }
    } catch (_: Exception) { null }

    fun text(strings: Strings, latestMs: Long?, now: Long, zone: ZoneId, locale: java.util.Locale): String {
        if (latestMs == null) return strings.get(R.string.daily_fresh_none)
        val hours = ((now - latestMs).coerceAtLeast(0L)) / 3_600_000L
        val time = DateTimeFormatter.ofLocalizedDateTime(FormatStyle.SHORT).withLocale(locale).withZone(zone)
            .format(Instant.ofEpochMilli(latestMs))
        val line = strings.get(R.string.daily_fresh_latest, time, hours.toInt())
        return if (hours >= GAP_HOURS) line + "\n" + strings.get(R.string.daily_fresh_gap) else line
    }
}
