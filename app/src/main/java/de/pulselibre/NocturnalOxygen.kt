package de.pulselibre

import java.time.Duration
import java.time.Instant

/**
 * Night-window SpO₂ summary (#39). PRODUCT ORIENTATION, NOT A DIAGNOSIS: wrist readings of the
 * CIRQA are not validated against a clinical pulse oximeter, and no sleep stage/interval is used —
 * the caller supplies a plain clock window. Every figure is withheld ("nicht berechenbar") unless
 * coverage and sampling density meet the gates below; nothing is interpolated.
 */
internal object NocturnalOxygen {
    data class Sample(val time: Instant, val percent: Int)

    /** Gates, deliberately strict; ODI additionally requires dense sampling. */
    val MIN_WINDOW: Duration = Duration.ofHours(4)
    const val MIN_COVERAGE_PERCENT = 70.0
    val MAX_HOLD: Duration = Duration.ofSeconds(120)   // a reading represents at most this long
    val ODI_MAX_MEDIAN_GAP: Duration = Duration.ofSeconds(30)
    val BASELINE_LOOKBACK: Duration = Duration.ofSeconds(120)

    data class Summary(
        val computable: Boolean,
        val reason: String,
        val samples: Int,
        val coveragePercent: Double?,
        val minutesBelow90: Double?,
        val odi3: Double?,
        val odi4: Double?
    )

    fun summarize(raw: List<Sample>, start: Instant, end: Instant): Summary {
        val window = Duration.between(start, end)
        fun no(reason: String, n: Int = 0, cov: Double? = null) = Summary(false, reason, n, cov, null, null, null)
        if (window < MIN_WINDOW) return no("Zeitfenster kürzer als ${MIN_WINDOW.toHours()} h.")
        val s = raw.filter { it.percent in 50..100 && !it.time.isBefore(start) && it.time.isBefore(end) }
            .sortedBy { it.time }.distinctBy { it.time }
        if (s.size < 2) return no("Zu wenige gültige Messwerte.", s.size)
        // Covered time: each reading represents up to MAX_HOLD, never beyond the next reading or the window end.
        var covered = Duration.ZERO
        var below = Duration.ZERO
        for (i in s.indices) {
            val next = if (i + 1 < s.size) s[i + 1].time else end
            val held = minOf(Duration.between(s[i].time, next), MAX_HOLD)
            covered += held
            if (s[i].percent < 90) below += held
        }
        val coverage = 100.0 * covered.toMillis() / window.toMillis()
        if (coverage < MIN_COVERAGE_PERCENT)
            return no("Abdeckung ${"%.0f".format(java.util.Locale.GERMANY, coverage)} % unter ${MIN_COVERAGE_PERCENT.toInt()} %.", s.size, coverage)
        val gaps = s.zipWithNext { a, b -> Duration.between(a.time, b.time) }.sorted()
        val medianGap = gaps[gaps.size / 2]
        val hours = window.toMillis() / 3_600_000.0
        val minutesBelow = below.toMillis() / 60_000.0
        if (medianGap > ODI_MAX_MEDIAN_GAP) {
            return Summary(true, "Abtastabstand zu groß für ODI (Median ${medianGap.seconds} s, nötig ≤ ${ODI_MAX_MEDIAN_GAP.seconds} s).",
                s.size, coverage, minutesBelow, null, null)
        }
        return Summary(true, "ok", s.size, coverage, minutesBelow, countEvents(s, 3) / hours, countEvents(s, 4) / hours)
    }

    /** A dip must last at least this long (consecutive readings below the threshold); shorter dips count as artifacts. */
    val MIN_EVENT_DURATION: Duration = Duration.ofSeconds(10)

    /**
     * Event = readings stay at least [drop] points below the maximum of the preceding lookback for
     * at least [MIN_EVENT_DURATION]. Single-sample dips (typical wrist-PPG motion artifacts) do not count.
     * The event is re-armed after recovery to within 1 point of the baseline.
     */
    internal fun countEvents(s: List<Sample>, drop: Int): Int {
        var events = 0
        var armed = true
        var dipStart: Instant? = null
        var dipCounted = false
        for (i in s.indices) {
            val from = s[i].time - BASELINE_LOOKBACK
            val baseline = s.subList(0, i).filter { it.time >= from }.maxOfOrNull { it.percent } ?: s[i].percent
            val below = baseline - s[i].percent >= drop
            if (below) {
                if (dipStart == null) { dipStart = s[i].time; dipCounted = false }
                if (armed && !dipCounted && Duration.between(dipStart, s[i].time) >= MIN_EVENT_DURATION) {
                    events++; dipCounted = true; armed = false
                }
            } else {
                dipStart = null
                if (!armed && baseline - s[i].percent <= 1) armed = true
            }
        }
        return events
    }
}
