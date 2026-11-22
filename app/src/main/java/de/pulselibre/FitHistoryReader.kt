package de.pulselibre

import android.content.Context
import java.time.Instant

/**
 * Read-only projection of band-measured heart rate from FIT imports for the "Verlauf" tab (#334).
 *
 * Points keep the band's MEASUREMENT time (`observations.measured_at_ms`). They are never combined with the
 * phone-reception series from [HistoryDataCache]; the two clocks mean different things. Only observations
 * with FIT provenance count, so legacy or live rows in the same table cannot leak into this series.
 */
internal object FitHistoryMapping {
    const val METRIC = "heart_rate"
    /** Cap per window; 30 days of 1 Hz data would be 2.6 million points, far beyond what a phone chart can use. */
    const val MAX_POINTS = 150_000

    fun toPoints(observations: List<HistoricalObservation>): List<HistoryPoint> =
        observations.asSequence()
            .filter { it.metric == METRIC && it.unit == "bpm" && it.provenance != null }
            .mapNotNull { o ->
                val bpm = Math.round(o.value)
                if (o.value.isFinite() && bpm in 1..240) HistoryPoint(o.measuredAt, bpm.toInt()) else null
            }
            .toList()
}

class FitHistoryReader(private val context: Context) {
    /** Result of one window read; [truncated] is true when [FitHistoryMapping.MAX_POINTS] cut the window. */
    data class Window(val points: List<HistoryPoint>, val available: Boolean, val truncated: Boolean,
                      /** Newest FIT heart-rate measurement of any time; explains an empty window (older data or a band clock offset). */
                      val newest: Instant? = null)

    /** Runs [block] on the unified measurements (live, import and band in one table), brought up to date first. */
    private fun <T> unified(block: (UnifiedMeasurements) -> T): T = CirqaDatabase(context).use { db ->
        db.ensureHistoricalStoreCopied()
        val unified = UnifiedMeasurements(db.writableDatabase)
        unified.refresh()
        block(unified)
    }

    fun read(start: Instant, end: Instant): Window = unified { u ->
        val rows = u.points(MeasurementOrigin.BAND, FitHistoryMapping.METRIC, start.toEpochMilli(), end.toEpochMilli(), FitHistoryMapping.MAX_POINTS + 1)
        val truncated = rows.size > FitHistoryMapping.MAX_POINTS
        val points = rows.asSequence().take(FitHistoryMapping.MAX_POINTS)
            .filter { it.third == "bpm" && it.second.isFinite() }
            .mapNotNull { (t, v, _) -> Math.round(v).takeIf { it in 1..240 }?.let { HistoryPoint(Instant.ofEpochMilli(t), it.toInt()) } }
            .toList()
        Window(points, u.exists(MeasurementOrigin.BAND, FitHistoryMapping.METRIC), truncated,
            u.latest(MeasurementOrigin.BAND, FitHistoryMapping.METRIC, Long.MAX_VALUE / 4)?.let { Instant.ofEpochMilli(it) })
    }

    /** Measurement time of the newest band heart-rate observation not after [notAfter], or null when there is none. */
    fun latestMeasuredAt(notAfter: Instant): Instant? = unified { u ->
        u.latest(MeasurementOrigin.BAND, FitHistoryMapping.METRIC, notAfter.toEpochMilli())?.let { Instant.ofEpochMilli(it) }
    }

    /** Newest band heart-rate measurement regardless of the window (also later than "now" if the band clock is ahead). */
    fun newestMeasuredAt(): Instant? = latestMeasuredAt(Instant.ofEpochMilli(Long.MAX_VALUE / 4))
}
