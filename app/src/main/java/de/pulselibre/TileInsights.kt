package de.pulselibre

import android.content.Context
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/** What a tile shows beyond its current value: today's range, a 24-hour sparkline and the 7-day same-time average. */
internal data class TileInsight(
    val metric: DashboardMeasurement,
    val cumulative: Boolean,
    val count: Int,
    val current: Double?,
    val min: Double?,
    val max: Double?,
    val average: Double?,
    /** 24 hourly values of the local day (hourly mean, or last counter value for cumulative metrics); null = no data. */
    val hourly: List<Double?>,
    /** Mean (or counter value) at this time of day over the previous days that have data. */
    val weekAtThisTime: Double?,
    val weekDays: Int,
    /** SpO₂ only: share of today's readings in the orientation bands (≥95, 90–94, <90), percent; else null. */
    val bands: Triple<Double, Double, Double>?,
    /** Heart rate only: today's values from band FIT files (band MEASUREMENT time); kept apart from the live values above (#334). */
    val fit: FitDaySummary? = null
) {
    val deltaToWeek: Double? get() = if (current != null && weekAtThisTime != null) current - weekAtThisTime else null
}

/** Today's band-measured (FIT) values of one metric; never combined with live reception-time values. */
internal data class FitDaySummary(val date: LocalDate, val count: Int, val min: Double, val max: Double, val average: Double, val hourly: List<Double?>)

internal object TileInsightCalc {
    /** Null when the band delivered nothing for [day] (up to [now]); hourly means use the band's measurement time. */
    fun summarizeFit(points: List<HistoryPoint>, day: DashboardDay, now: Instant): FitDaySummary? {
        val valid = points.filter { day.includes(it.receivedAt, now) }
        if (valid.isEmpty()) return null
        val hourly = arrayOfNulls<Double>(24)
        valid.groupBy { it.receivedAt.atZone(day.zone).hour }.forEach { (h, list) -> hourly[h] = list.map { it.value.toDouble() }.average() }
        val values = valid.map { it.value.toDouble() }
        return FitDaySummary(day.date, values.size, values.min(), values.max(), values.average(), hourly.toList())
    }

    val CUMULATIVE = setOf(DashboardMeasurement.STEPS, DashboardMeasurement.CALORIES_TOTAL,
        DashboardMeasurement.CALORIES_ACTIVE, DashboardMeasurement.MODERATE, DashboardMeasurement.VIGOROUS)

    /** Minimum number of past days with data before a same-time average is shown. */
    const val MIN_WEEK_DAYS = 3

    fun summarize(metric: DashboardMeasurement, points: List<DayObservation>, day: DashboardDay, now: Instant,
                  weekValues: List<Double>): TileInsight {
        val cumulative = metric in CUMULATIVE
        val valid = points.filter { day.includes(it.time, now) }.sortedBy { it.time }
        val values = valid.map { it.value.toDouble() }
        val hourly = arrayOfNulls<Double>(24)
        val zone = day.zone
        val byHour = valid.groupBy { it.time.atZone(zone).hour }
        for ((h, list) in byHour) hourly[h] = if (cumulative) list.maxOf { it.value }.toDouble() else list.map { it.value.toDouble() }.average()
        val bands = if (metric == DashboardMeasurement.OXYGEN && values.isNotEmpty()) {
            val n = values.size.toDouble()
            Triple(100.0 * values.count { it >= 95 } / n, 100.0 * values.count { it in 90.0..94.999 } / n, 100.0 * values.count { it < 90 } / n)
        } else null
        val week = if (weekValues.size >= MIN_WEEK_DAYS) weekValues.average() else null
        return TileInsight(metric, cumulative, values.size, values.lastOrNull(), values.minOrNull(), values.maxOrNull(),
            if (values.isEmpty()) null else if (cumulative) values.last() else values.average(),
            hourly.toList(), week, weekValues.size, bands)
    }

    /** Whole local days before [today] (1..[days]) with the time-of-day window around [now]. */
    fun pastWindows(now: Instant, zone: ZoneId, days: Int = 7, halfWidth: Duration = Duration.ofMinutes(30)): List<Pair<Instant, Instant>> {
        val local = now.atZone(zone)
        return (1..days).map { d ->
            val center = local.minusDays(d.toLong()).toInstant()
            center.minus(halfWidth) to center.plus(halfWidth)
        }
    }

    fun format(value: Double, unit: String): String {
        val text = if (value >= 100 || value == Math.rint(value)) String.format(java.util.Locale.GERMANY, "%,.0f", value)
            else String.format(java.util.Locale.GERMANY, "%.1f", value)
        return if (unit.isBlank()) text else "$text $unit"
    }
}

internal class TileInsightReader(private val context: Context) {
    fun read(metric: DashboardMeasurement, zone: ZoneId, now: Instant): TileInsight {
        val day = DashboardDay(LocalDate.ofEpochDay(now.atZone(zone).toLocalDate().toEpochDay()), zone)
        val today = DashboardDayReader(context).read(metric, day, now).points
        val cumulative = metric in TileInsightCalc.CUMULATIVE
        val weekValues = ArrayList<Double>()
        CirqaDatabase(context).use { db ->
            for ((from, to) in TileInsightCalc.pastWindows(now, zone)) {
                val values = ArrayList<Double>()
                val sql = if (cumulative) {
                    // counter value at this time of day = last reading of that day up to the window end
                    val dayStart = from.atZone(zone).toLocalDate().atStartOfDay(zone).toInstant()
                    """SELECT payload_json FROM events WHERE kind = 'reading' AND service = ? AND dashboard_time >= ? AND dashboard_time <= ?
                       AND payload_json LIKE ? ORDER BY dashboard_time DESC LIMIT 1""" to arrayOf(metric.service.toString(),
                        CirqaDatabase.timeKey(dayStart), CirqaDatabase.timeKey(to), "%\"${metric.key}\"%")
                } else {
                    """SELECT payload_json FROM events WHERE kind = 'reading' AND service = ? AND dashboard_time >= ? AND dashboard_time <= ?
                       AND payload_json LIKE ?""" to arrayOf(metric.service.toString(), CirqaDatabase.timeKey(from),
                        CirqaDatabase.timeKey(to), "%\"${metric.key}\"%")
                }
                db.readableDatabase.rawQuery(sql.first, sql.second).use { c ->
                    while (c.moveToNext()) {
                        val json = runCatching { org.json.JSONObject(c.getString(0)) }.getOrNull() ?: continue
                        metric.value(json)?.let { values.add(it.toDouble()) }
                    }
                }
                if (values.isNotEmpty()) weekValues.add(if (cumulative) values.last() else values.average())
            }
        }
        val fit = if (metric != DashboardMeasurement.PULSE) null else try {
            val reader = FitHistoryReader(context)
            // Band history is usually older than today: show today's band values, else the newest day that has some (labelled with its date).
            val latest = reader.latestMeasuredAt(now)
            if (latest == null) null else {
                val fitDay = if (latest >= day.start) day else DashboardDay(latest.atZone(zone).toLocalDate(), zone)
                val window = reader.read(fitDay.start, maxOf(fitDay.start.plusSeconds(1), minOf(fitDay.end, now.plusSeconds(60))))
                TileInsightCalc.summarizeFit(window.points, fitDay, now)
            }
        } catch (_: Exception) { null }
        return TileInsightCalc.summarize(metric, today, day, now, weekValues).copy(fit = fit)
    }
}
