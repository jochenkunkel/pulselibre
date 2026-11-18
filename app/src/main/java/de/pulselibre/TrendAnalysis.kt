package de.pulselibre

import java.time.LocalDate

/**
 * Explainable, non-diagnostic trend of one daily value (#41).
 * Current window = the [windowDays] complete local days before [today]; baseline = the window of
 * equal length directly before it. Only days that actually have a value count; missing days are
 * reported, never filled. The median is robust against single outliers.
 */
internal object TrendAnalysis {
    enum class Direction(val arrow: String, val label: String) {
        UP("↑", "höher"), DOWN("↓", "niedriger"), STABLE("→", "unverändert"), UNKNOWN("–", "nicht bewertbar")
    }

    data class Window(val days: Int, val validDays: Int, val median: Double?) {
        val missingDays: Int get() = days - validDays
        val coveragePercent: Double get() = 100.0 * validDays / days
    }

    data class Result(
        val windowDays: Int,
        val current: Window,
        val baseline: Window,
        val delta: Double?,
        val direction: Direction,
        val reason: String
    )

    /** Minimum valid days per window: at least half the window, never fewer than 3. */
    fun minValidDays(windowDays: Int): Int = maxOf(3, (windowDays + 1) / 2)

    /** Differences smaller than this share of the baseline median (or the absolute floor) count as stable. */
    private const val RELATIVE_STABLE = 0.03

    fun analyze(values: Map<LocalDate, Double>, today: LocalDate, windowDays: Int, absoluteFloor: Double): Result {
        require(windowDays > 0 && absoluteFloor >= 0)
        fun window(endExclusive: LocalDate): Window {
            val days = (1..windowDays).map { endExclusive.minusDays(it.toLong()) }
            val present = days.mapNotNull { values[it]?.takeIf(Double::isFinite) }
            return Window(windowDays, present.size, if (present.isEmpty()) null else median(present))
        }
        val current = window(today)
        val baseline = window(today.minusDays(windowDays.toLong()))
        val need = minValidDays(windowDays)
        val cm = current.median
        val bm = baseline.median
        if (current.validDays < need || cm == null)
            return Result(windowDays, current, baseline, null, Direction.UNKNOWN,
                "Zu wenige Tage mit Daten (${current.validDays} von mindestens $need).")
        if (baseline.validDays < need || bm == null)
            return Result(windowDays, current, baseline, null, Direction.UNKNOWN,
                "Vergleichszeitraum hat zu wenige Daten (${baseline.validDays} von mindestens $need).")
        val delta = cm - bm
        val threshold = maxOf(absoluteFloor, RELATIVE_STABLE * kotlin.math.abs(bm))
        val direction = when {
            delta > threshold -> Direction.UP
            delta < -threshold -> Direction.DOWN
            else -> Direction.STABLE
        }
        return Result(windowDays, current, baseline, delta, direction, "Median der letzten $windowDays Tage gegen die $windowDays Tage davor.")
    }

    fun median(values: List<Double>): Double {
        require(values.isNotEmpty())
        val s = values.sorted()
        val mid = s.size / 2
        return if (s.size % 2 == 1) s[mid] else (s[mid - 1] + s[mid]) / 2.0
    }
}
