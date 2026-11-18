package de.pulselibre

import java.time.Duration
import java.time.Instant

/**
 * Observed-data quality for one local day (#40). Only real observation times are used;
 * gaps are reported, never filled. Gap = time between consecutive observations (and from
 * day start / last observation to "now") that exceeds [gapThreshold].
 */
internal data class DayDataQuality(
    val observedMinutes: Int,
    val availableMinutes: Int,
    val longestGap: Duration?,
    val gapCount: Int,
) {
    /** Share of elapsed minutes with at least one observation; null if the day has not started. */
    val coveragePercent: Double? =
        if (availableMinutes <= 0) null else 100.0 * observedMinutes / availableMinutes

    fun summary(): String {
        val coverage = coveragePercent ?: return "Tag noch nicht begonnen"
        val text = "Abdeckung ${"%.0f".format(java.util.Locale.GERMANY, coverage)} % (${observedMinutes} von ${availableMinutes} min)"
        return if (longestGap == null) text
        else "$text · $gapCount Lücke${if (gapCount == 1) "" else "n"}, längste ${longestGap.toMinutes()} min"
    }

    companion object {
        val DEFAULT_GAP: Duration = Duration.ofMinutes(5)

        fun assess(times: List<Instant>, day: DashboardDay, now: Instant,
                   gapThreshold: Duration = DEFAULT_GAP): DayDataQuality {
            val valid = times.filter { day.includes(it, now) }.sorted().distinct()
            val minutes = valid.map { Duration.between(day.start, it).toMinutes() }.distinct().size
            val available = day.availableMinutes(now)
            if (available <= 0) return DayDataQuality(0, 0, null, 0)
            val edgeEnd = minOf(now, day.end)
            val boundaries = listOf(day.start) + valid + listOf(edgeEnd)
            val gaps = boundaries.zipWithNext { a, b -> Duration.between(a, b) }.filter { it > gapThreshold }
            return DayDataQuality(minutes, available, gaps.maxOrNull(), gaps.size)
        }
    }
}
