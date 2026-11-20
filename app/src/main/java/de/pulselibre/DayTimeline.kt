package de.pulselibre

import java.time.Duration
import java.time.Instant

/**
 * Model of the unified 24-hour view (#263): several metrics on **one** local-day time axis.
 * Pure and Android-free. Only real observation times are used; gaps are reported, never bridged or filled.
 */
internal object DayTimeline {
    data class Gap(val start: Instant, val end: Instant) {
        val duration: Duration get() = Duration.between(start, end)
    }

    class Lane(
        val metric: DashboardMeasurement,
        val title: String,
        val unit: String,
        /** Cumulative counter (steps): drawn as a staircase and never as an average. */
        val cumulative: Boolean,
        val gapThreshold: Duration,
        points: List<DayObservation>,
        day: DashboardDay,
        now: Instant,
    ) {
        val points: List<DayObservation> = points.filter { day.includes(it.time, now) }.sortedBy { it.time }
        val gaps: List<Gap> = gapsOf(this.points, day, now, gapThreshold)
        val min: Long? = this.points.minOfOrNull { it.value }
        val max: Long? = this.points.maxOfOrNull { it.value }

        /** Index of the observation nearest to [time] within the gap threshold, or null (= no data there). */
        fun nearestIndex(time: Instant): Int? {
            if (points.isEmpty()) return null
            val i = points.indices.minBy { Duration.between(points[it].time, time).abs() }
            return i.takeIf { Duration.between(points[i].time, time).abs() <= gapThreshold }
        }

        /** Whether the line may connect point [i] to point i+1 (close in time; counters never connect across a reset). */
        fun connectsToNext(i: Int): Boolean {
            if (i < 0 || i + 1 >= points.size) return false
            val a = points[i]; val b = points[i + 1]
            if (Duration.between(a.time, b.time) > gapThreshold) return false
            return !cumulative || b.value >= a.value
        }
    }

    /** Gaps = time without observation longer than [threshold], including day start → first and last → now. */
    fun gapsOf(sorted: List<DayObservation>, day: DashboardDay, now: Instant, threshold: Duration): List<Gap> {
        if (day.availableMinutes(now) <= 0) return emptyList()
        val edgeEnd = minOf(now, day.end)
        val times = sorted.map { it.time }.distinct()
        return (listOf(day.start) + times + listOf(edgeEnd)).zipWithNext { a, b -> Gap(a, b) }
            .filter { it.duration > threshold }
    }

    fun lane(metric: DashboardMeasurement, title: String, unit: String, points: List<DayObservation>,
             day: DashboardDay, now: Instant): Lane {
        val cumulative = metric == DashboardMeasurement.STEPS
        val threshold = if (cumulative) StepIntervals.MAX_GAP else DayDataQuality.DEFAULT_GAP
        return Lane(metric, title, unit, cumulative, threshold, points, day, now)
    }

    /** Text version of the whole view (TalkBack, and shown under the chart). */
    fun describe(lanes: List<Lane>, day: DashboardDay, now: Instant, zone: java.time.ZoneId): String {
        if (day.availableMinutes(now) <= 0) return "Tag noch nicht begonnen."
        val fmt = java.time.format.DateTimeFormatter.ofPattern("HH:mm").withZone(zone)
        return lanes.joinToString("\n") { lane ->
            if (lane.points.isEmpty()) "${lane.title}: keine Daten an diesem Tag."
            else {
                val range = if (lane.cumulative) "${lane.points.last().value}${if (lane.unit.isEmpty()) "" else " " + lane.unit} zuletzt"
                    else "${lane.min}–${lane.max} ${lane.unit}"
                val longest = lane.gaps.maxByOrNull { it.duration }
                val gapText = if (longest == null) "keine Lücken"
                    else "${lane.gaps.size} Lücke${if (lane.gaps.size == 1) "" else "n"}, längste " +
                        "${longest.duration.toMinutes()} min (${fmt.format(longest.start)}–${fmt.format(longest.end)})"
                "${lane.title}: $range, ${lane.points.size} Messwerte, $gapText."
            }
        }
    }
}

/**
 * Visible part of the day axis as fractions 0..1 (#264). Zooming never changes the data, only which part of the
 * time axis is shown; the smallest window is [MIN_SPAN] (≈ 15 minutes of a 24-hour day).
 */
internal data class DayViewport(val start: Float = 0f, val end: Float = 1f) {
    val span: Float get() = end - start
    val isFull: Boolean get() = start <= 0f && end >= 1f

    /** Zoom by [factor] (> 1 = in) keeping the day fraction [focus] at the same screen position. */
    fun zoom(focus: Float, factor: Float): DayViewport {
        if (factor <= 0f || factor.isNaN()) return this
        val newSpan = (span / factor).coerceIn(MIN_SPAN, 1f)
        val f = ((focus - start) / span).coerceIn(0f, 1f)
        return place(focus - f * newSpan, newSpan)
    }

    /** Move by [delta] day fractions; stays inside the day. */
    fun pan(delta: Float): DayViewport = place(start + delta, span)

    private fun place(newStart: Float, newSpan: Float): DayViewport {
        val s = newStart.coerceIn(0f, 1f - newSpan)
        return DayViewport(s, s + newSpan)
    }

    companion object {
        const val MIN_SPAN = 15f / (24 * 60)

        /** Tick spacing in minutes so that roughly 3–6 labels fit the visible span of a day of [dayMinutes]. */
        fun tickMinutes(span: Float, dayMinutes: Long): Int {
            val visible = span * dayMinutes
            return when {
                visible > 12 * 60 -> 360
                visible > 6 * 60 -> 180
                visible > 3 * 60 -> 60
                visible > 60 -> 30
                else -> 10
            }
        }
    }
}
