package de.pulselibre

import java.text.NumberFormat
import java.time.LocalDate
import java.util.Locale

/**
 * Resting-pulse hero model (#41 "Pulse Flow"). Input is ONLY the device resting pulse per day (never the lowest daily
 * pulse); a missing day stays a gap (no line across it, no estimate). The delta compares with the directly preceding,
 * equally long, non-overlapping period and is worded neutrally (no "fitter"/"better").
 */
internal object RestingPulseHero {
    enum class State { OK, INSUFFICIENT, NO_DATA }

    data class Point(val date: LocalDate, val dayIndex: Int, val bpm: Double)

    data class Model(
        val windowDays: Int,
        val state: State,
        val points: List<Point>,
        /** Runs of consecutive valid days; only inside a run the chart draws a line. */
        val segments: List<List<Point>>,
        val valueText: String?,
        val deltaText: String?,
        val baselineBpm: Double?,
        val message: String,
        val validDays: Int,
        val firstDay: LocalDate,
        val lastDay: LocalDate
    ) {
        fun description(): String = buildString {
            append("Ruhepuls, Entwicklung über $windowDays Tage. ")
            if (valueText != null) append("Median $valueText. ")
            if (deltaText != null) append("$deltaText. ")
            append(message)
        }
    }

    private val one = NumberFormat.getNumberInstance(Locale.GERMANY).apply { maximumFractionDigits = 1 }

    /** [trend] must be computed from the same [daily] map and window (as done by the trend card). */
    fun build(daily: Map<LocalDate, Double>, today: LocalDate, windowDays: Int, trend: TrendAnalysis.Result): Model {
        require(trend.windowDays == windowDays)
        val first = today.minusDays(windowDays.toLong())
        val points = (0 until windowDays).mapNotNull { i ->
            val d = first.plusDays(i.toLong())
            daily[d]?.takeIf { it.isFinite() }?.let { Point(d, i, it) }
        }
        val segments = ArrayList<MutableList<Point>>()
        points.forEach { p ->
            val last = segments.lastOrNull()?.last()
            if (last != null && last.dayIndex + 1 == p.dayIndex) segments.last().add(p) else segments.add(mutableListOf(p))
        }
        val lastDay = today.minusDays(1)
        val need = TrendAnalysis.minValidDays(windowDays)
        if (points.isEmpty()) return Model(windowDays, State.NO_DATA, points, segments, null, null, null,
            "Noch keine verifizierten Ruhepulswerte. Quelle: vom Gerät berechneter Ruhepuls (kein Ersatz durch Live-Puls).",
            0, first, lastDay)
        if (trend.current.validDays < need || trend.current.median == null) return Model(windowDays, State.INSUFFICIENT, points, segments,
            null, null, null,
            "Zu wenige Tage mit Ruhepuls (${trend.current.validDays} von mindestens $need): Tageswerte werden gezeigt, aber keine Trend- oder Vergleichsaussage.",
            trend.current.validDays, first, lastDay)
        val median = trend.current.median
        val baselineOk = trend.baseline.validDays >= need && trend.baseline.median != null
        val delta = if (baselineOk) trend.delta else null
        val deltaText = delta?.let {
            val rounded = Math.round(it * 10) / 10.0
            val sign = when { rounded > 0 -> "+"; rounded < 0 -> "−"; else -> "±" }
            "$sign${one.format(Math.abs(rounded))} bpm gegenüber vorherigen $windowDays Tagen"
        }
        val message = if (baselineOk) "Vom Gerät berechneter Ruhepuls; Vergleich mit den $windowDays Tagen davor. Keine Fitnessdiagnose."
            else "Vom Gerät berechneter Ruhepuls. Vergleichszeitraum hat zu wenige Daten (${trend.baseline.validDays} von mindestens $need); kein Vergleich."
        return Model(windowDays, State.OK, points, segments, "${one.format(median)} bpm", deltaText,
            if (baselineOk) trend.baseline.median else null, message, trend.current.validDays, first, lastDay)
    }
}
