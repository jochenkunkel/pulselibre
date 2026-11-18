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

internal enum class DeviceSource(val title: String, val icon: String, val service: Int) {
    PULSE("Herzfrequenz", "💓", 6), OXYGEN("SpO₂", "🫁", 19), STEPS("Schritte", "👣", 7), STRESS("Stress", "🧠", 13),
    BATTERY("Body Battery", "🔋", 20), RR("RR-Intervall", "〰", 12), BREATH("Atmung", "🌬", 21),
    CALORIES("Kalorien", "🔥", 8), INTENSITY("Intensität", "⏱", 10)
}

internal object DeviceFormat {
    fun age(last: Instant?, now: Instant): String {
        if (last == null || last.isAfter(now)) return "kein Empfang"
        val s = Duration.between(last, now).seconds
        return when {
            s < 60 -> "vor $s s"
            s < 3600 -> "vor ${s / 60} min"
            s < 86_400 -> "vor ${s / 3600} h ${(s % 3600) / 60} min"
            else -> "vor ${s / 86_400} Tagen"
        }
    }
    fun bytes(b: Long): String = when {
        b >= 1_048_576 -> String.format(java.util.Locale.GERMANY, "%.1f MB", b / 1_048_576.0)
        b >= 1024 -> String.format(java.util.Locale.GERMANY, "%d KB", b / 1024)
        else -> "$b B"
    }
    fun count(n: Long): String = java.text.NumberFormat.getIntegerInstance(java.util.Locale.GERMANY).format(n)
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
