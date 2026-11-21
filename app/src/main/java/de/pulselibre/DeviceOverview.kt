package de.pulselibre

import android.content.Context
import java.time.Duration
import java.time.Instant
import java.time.ZoneId

/** Real, locally stored facts about the band link; nothing here is guessed or read from the band. */
internal data class DeviceStats(
    val lastBySource: Map<DeviceSource, Instant?>,
    val readingsLastHour: Int,
    val sessionsToday: Int,
    val reconnectsToday: Int,
    val totalEvents: Long,
    val databaseBytes: Long,
    val firstReading: Instant?
)

internal enum class DeviceSource(val titleRes: Int, val icon: PlIcon, val service: Int) {
    PULSE(R.string.dev_src_pulse, PlIcon.HEART, 6), OXYGEN(R.string.dev_src_oxygen, PlIcon.OXYGEN, 19), STEPS(R.string.dev_src_steps, PlIcon.STEPS, 7),
    STRESS(R.string.dev_src_stress, PlIcon.STRESS, 13), BATTERY(R.string.dev_src_battery, PlIcon.BODY_BATTERY, 20), RR(R.string.dev_src_rr, PlIcon.RR_INTERVAL, 12),
    BREATH(R.string.dev_src_breath, PlIcon.BREATHING, 21), CALORIES(R.string.dev_src_calories, PlIcon.CALORIES, 8),
    INTENSITY(R.string.dev_src_intensity, PlIcon.INTENSITY, 10)
}

internal object DeviceFormat {
    fun age(strings: Strings, last: Instant?, now: Instant): String {
        if (last == null || last.isAfter(now)) return strings.get(R.string.dev_no_reception)
        val s = Duration.between(last, now).seconds
        return when {
            s < 60 -> strings.get(R.string.dur_ago, strings.get(R.string.dur_s, s))
            s < 3600 -> strings.get(R.string.dur_ago, strings.get(R.string.dur_min, s / 60))
            s < 86_400 -> strings.get(R.string.dur_ago, strings.get(R.string.dur_h_min, s / 3600, (s % 3600) / 60))
            s < 2 * 86_400 -> strings.get(R.string.dur_ago_day)
            else -> strings.get(R.string.dur_ago_days, s / 86_400)
        }
    }
    fun bytes(b: Long, locale: java.util.Locale): String = when {
        b >= 1_048_576 -> String.format(locale, "%.1f MB", b / 1_048_576.0)
        b >= 1024 -> String.format(locale, "%d KB", b / 1024)
        else -> "$b B"
    }
    fun count(n: Long, locale: java.util.Locale): String = java.text.NumberFormat.getIntegerInstance(locale).format(n)
}

internal class DeviceStatsReader(private val context: Context) {
    fun read(now: Instant, zone: ZoneId): DeviceStats {
        val last = LinkedHashMap<DeviceSource, Instant?>()
        var lastHour = 0; var sessions = 0; var reconnects = 0; var total = 0L; var first: Instant? = null
        val startOfToday = now.atZone(zone).toLocalDate().atStartOfDay(zone).toInstant()
        CirqaDatabase(context).use { helper ->
            val db = helper.readableDatabase
            for (src in DeviceSource.entries) {
                last[src] = db.rawQuery("SELECT MAX(dashboard_time) FROM events WHERE kind = 'reading' AND service = ?",
                    arrayOf(src.service.toString())).use { c ->
                    if (c.moveToFirst() && !c.isNull(0)) runCatching { Instant.parse(c.getString(0)) }.getOrNull() else null
                }
            }
            db.rawQuery("SELECT COUNT(*) FROM events WHERE kind = 'reading' AND dashboard_time >= ?",
                arrayOf(CirqaDatabase.timeKey(now.minus(Duration.ofHours(1))))).use { if (it.moveToFirst()) lastHour = it.getInt(0) }
            db.rawQuery("SELECT kind, COUNT(*) FROM events WHERE kind IN ('session_start','reconnect_scheduled') AND received_at_utc >= ? GROUP BY kind",
                arrayOf(startOfToday.toString())).use { c ->
                while (c.moveToNext()) when (c.getString(0)) { "session_start" -> sessions = c.getInt(1); else -> reconnects = c.getInt(1) }
            }
            db.rawQuery("SELECT COUNT(*) FROM events", null).use { if (it.moveToFirst()) total = it.getLong(0) }
            db.rawQuery("SELECT MIN(received_at_utc) FROM events WHERE kind = 'reading'", null).use {
                if (it.moveToFirst() && !it.isNull(0)) first = runCatching { Instant.parse(it.getString(0)) }.getOrNull()
            }
        }
        val size = context.getDatabasePath("cirqa_measurements.db").length()
        return DeviceStats(last, lastHour, sessions, reconnects, total, size, first)
    }
}
