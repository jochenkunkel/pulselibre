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
    data class Window(val points: List<HistoryPoint>, val available: Boolean, val truncated: Boolean)

    fun read(start: Instant, end: Instant): Window {
        val repository = HistoricalObservationRepository(context)
        val points = ArrayList<HistoryPoint>()
        var cursor: HistoricalObservationQuery.Cursor? = null
        var truncated = false
        while (true) {
            val page = repository.read(HistoricalObservationQuery(
                startInclusive = start, endExclusive = end, metric = FitHistoryMapping.METRIC,
                limit = 5_000, after = cursor))
            points += FitHistoryMapping.toPoints(page.observations)
            cursor = page.next ?: break
            if (points.size >= FitHistoryMapping.MAX_POINTS) { truncated = true; break }
        }
        return Window(points, available(), truncated)
    }

    /**
     * Measurement time of the newest FIT heart-rate observation not after [notAfter], or null when there is none.
     * Reads the active decoder generations (the production import path) and the legacy provenance table (#477);
     * the legacy table alone is empty after a product download.
     */
    fun latestMeasuredAt(notAfter: Instant): Instant? = CirqaDatabase(context).use { db ->
        db.ensureHistoricalStoreCopied()
        val limit = notAfter.toEpochMilli().toString()
        db.readableDatabase.rawQuery(
            """SELECT MAX(t) FROM (
                 SELECT MAX(g.measured_at_ms) AS t FROM fit_generation_observations g
                   JOIN fit_active_generations a ON a.generation_id = g.generation_id
                   WHERE g.metric = ? AND g.measured_at_ms <= ?
                 UNION ALL
                 SELECT MAX(measured_at_ms) FROM fit_observation_provenance WHERE metric = ? AND measured_at_ms <= ?
               )""",
            arrayOf(FitHistoryMapping.METRIC, limit, FitHistoryMapping.METRIC, limit)
        ).use { if (it.moveToFirst() && !it.isNull(0)) Instant.ofEpochMilli(it.getLong(0)) else null }
    }

    /** True when any FIT heart-rate observation exists, in an active generation or the legacy table (#477, A6-28). */
    private fun available(): Boolean = CirqaDatabase(context).use { db ->
        db.ensureHistoricalStoreCopied()
        db.readableDatabase.rawQuery(
            """SELECT 1 WHERE EXISTS (
                 SELECT 1 FROM fit_generation_observations g
                   JOIN fit_active_generations a ON a.generation_id = g.generation_id WHERE g.metric = ?)
               OR EXISTS (SELECT 1 FROM fit_observation_provenance WHERE metric = ?)""",
            arrayOf(FitHistoryMapping.METRIC, FitHistoryMapping.METRIC)
        ).use { it.moveToFirst() }
    }
}
