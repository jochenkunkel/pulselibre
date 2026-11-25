package de.pulselibre

import android.content.Context
import org.json.JSONObject
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/**
 * One value per local day for trend analysis (#41), from stored device fields only.
 * Resting pulse: median of the device-reported `resting_bpm` that day (no day minimum of the pulse).
 * Steps: highest cumulative daily counter reading of that day.
 */
internal object DailyAggregation {
    /**
     * An exact repeat of one reading (same reception time, same value, e.g. replayed after a reconnect) counts once,
     * as in the day view ([DashboardDayReader]); otherwise it would weigh twice in the resting-pulse median.
     */
    fun group(rows: List<Pair<Instant, Double>>, zone: ZoneId, steps: Boolean): Map<LocalDate, Double> =
        rows.distinct().groupBy({ it.first.atZone(zone).toLocalDate() }, { it.second }).mapValues { (_, v) ->
            if (steps) v.max() else TrendAnalysis.median(v)
        }
}

internal class DailyValueReader(private val context: Context) {
    fun read(metric: DashboardMeasurement, firstDay: LocalDate, today: LocalDate, zone: ZoneId): Map<LocalDate, Double> {
        require(metric == DashboardMeasurement.RESTING_PULSE || metric == DashboardMeasurement.STEPS)
        val start = firstDay.atStartOfDay(zone).toInstant()
        val end = today.atStartOfDay(zone).toInstant() // complete days only
        val rows = ArrayList<Pair<Instant, Double>>()
        CirqaDatabase(context).use { db ->
            db.readableDatabase.rawQuery(
                """SELECT payload_json FROM events WHERE kind = 'reading' AND service = ?
                   AND dashboard_time >= ? AND dashboard_time < ? AND payload_json LIKE ?""",
                arrayOf(metric.service.toString(), CirqaDatabase.timeKey(start), CirqaDatabase.timeKey(end),
                    "%\"${metric.key}\"%")
            ).use { c ->
                while (c.moveToNext()) {
                    val event = runCatching { JSONObject(c.getString(0)) }.getOrNull() ?: continue
                    val time = runCatching { Instant.parse(event.getString("received_at_utc")) }.getOrNull() ?: continue
                    val value = metric.value(event) ?: continue
                    if (time >= start && time < end) rows.add(time to value.toDouble())
                }
            }
        }
        return DailyAggregation.group(rows, zone, steps = metric == DashboardMeasurement.STEPS)
    }
}
