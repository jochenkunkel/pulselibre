package de.pulselibre

import android.content.Context
import org.json.JSONObject
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId

/** Raw SpO₂ readings of the last completed clock night (22:00–06:00 local); no sleep intervals involved. */
internal class NightOxygenReader(private val context: Context) {
    data class Night(val start: Instant, val end: Instant, val samples: List<NocturnalOxygen.Sample>)

    fun lastNightWindow(now: Instant, zone: ZoneId): Pair<Instant, Instant> {
        val today = LocalDate.ofInstant(now, zone)
        val endDay = if (now.atZone(zone).toLocalTime() >= LocalTime.of(6, 0)) today else today.minusDays(1)
        val end = endDay.atTime(6, 0).atZone(zone).toInstant()
        val start = endDay.minusDays(1).atTime(22, 0).atZone(zone).toInstant()
        return start to end
    }

    fun read(now: Instant, zone: ZoneId): Night {
        val (start, end) = lastNightWindow(now, zone)
        val metric = DashboardMeasurement.OXYGEN
        val out = ArrayList<NocturnalOxygen.Sample>()
        CirqaDatabase(context).use { db ->
            db.readableDatabase.rawQuery(
                """SELECT payload_json FROM events WHERE kind = 'reading' AND service = ?
                   AND dashboard_time >= ? AND dashboard_time < ? AND payload_json LIKE ?""",
                arrayOf(metric.service.toString(), CirqaDatabase.timeKey(start), CirqaDatabase.timeKey(end),
                    "%\"${metric.key}\"%")
            ).use { c ->
                while (c.moveToNext()) {
                    val e = runCatching { JSONObject(c.getString(0)) }.getOrNull() ?: continue
                    val t = runCatching { Instant.parse(e.getString("received_at_utc")) }.getOrNull() ?: continue
                    val v = metric.value(e) ?: continue
                    out.add(NocturnalOxygen.Sample(t, v.toInt()))
                }
            }
        }
        return Night(start, end, out)
    }
}
