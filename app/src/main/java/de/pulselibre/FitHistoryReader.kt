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

/**
 * Band (FIT) series a day dialog can show next to the live values. Keys and units are the canonical decoder contract of
 * #696 (Garmin profile 21.222.0); status codes never reach these metrics. Respiration is rounded to whole breaths/min
 * for the chart only.
 */
internal enum class BandSeries(val metric: String, val unit: String, val range: IntRange) {
    HEART_RATE("heart_rate", "bpm", 1..240),
    SPO2("spo2", "percent", 1..100),
    STRESS("stress", "device_scale", 1..100),
    RESPIRATION("respiration", "breaths/min", 1..100);

    companion object {
        fun of(measurement: DashboardMeasurement): BandSeries? = when (measurement) {
            DashboardMeasurement.PULSE -> HEART_RATE
            DashboardMeasurement.OXYGEN -> SPO2
            DashboardMeasurement.STRESS -> STRESS
            DashboardMeasurement.RESPIRATION -> RESPIRATION
            else -> null
        }
    }
}

class FitHistoryReader(private val context: Context) {
    /** Result of one window read; [truncated] is true when [FitHistoryMapping.MAX_POINTS] cut the window. */
    data class Window(val points: List<HistoryPoint>, val available: Boolean, val truncated: Boolean,
                      /** Newest FIT heart-rate measurement of any time; explains an empty window (older data or a band clock offset). */
                      val newest: Instant? = null,
                      /** Values of the window per FIT message kind: normal all-day history vs. recorded activities. */
                      val kinds: Map<BandRecordKind, Int> = emptyMap(),
                      /** Values of the window also contained in another FIT file, counted once (R1). */
                      val duplicates: Int = 0)

    /** Runs [block] on the unified measurements (live, import and band in one table), brought up to date first. */
    private fun <T> unified(block: (UnifiedMeasurements) -> T): T = CirqaDatabase(context).use { db ->
        db.ensureHistoricalStoreCopied()
        val unified = UnifiedMeasurements(db.writableDatabase)
        unified.refresh()
        block(unified)
    }

    fun read(start: Instant, end: Instant): Window = read(BandSeries.HEART_RATE, start, end)

    /** Band values of [series] in [start, end): only the canonical FIT metric, unit and measurement range (#696). */
    internal fun read(series: BandSeries, start: Instant, end: Instant): Window = unified { u ->
        val rows = u.points(MeasurementOrigin.BAND, series.metric, start.toEpochMilli(), end.toEpochMilli(), FitHistoryMapping.MAX_POINTS + 1)
        val truncated = rows.size > FitHistoryMapping.MAX_POINTS
        val points = rows.asSequence().take(FitHistoryMapping.MAX_POINTS)
            .filter { it.third == series.unit && it.second.isFinite() && it.second > 0 }
            .mapNotNull { (t, v, _) -> Math.round(v).takeIf { it in series.range }?.let { HistoryPoint(Instant.ofEpochMilli(t), it.toInt()) } }
            .toList()
        Window(points, u.exists(MeasurementOrigin.BAND, series.metric), truncated,
            u.latest(MeasurementOrigin.BAND, series.metric, Long.MAX_VALUE / 4)?.let { Instant.ofEpochMilli(it) },
            u.bandKindCounts(series.metric, start.toEpochMilli(), end.toEpochMilli()),
            u.bandDuplicateCount(series.metric, start.toEpochMilli(), end.toEpochMilli()))
    }

    /** Measurement time of the newest band heart-rate observation not after [notAfter], or null when there is none. */
    fun latestMeasuredAt(notAfter: Instant): Instant? = unified { u ->
        u.latest(MeasurementOrigin.BAND, FitHistoryMapping.METRIC, notAfter.toEpochMilli())?.let { Instant.ofEpochMilli(it) }
    }

    /** Newest band heart-rate measurement regardless of the window (also later than "now" if the band clock is ahead). */
    fun newestMeasuredAt(): Instant? = latestMeasuredAt(Instant.ofEpochMilli(Long.MAX_VALUE / 4))
}

/** Honest wording of where band values come from: normal all-day history and activity recordings are not the same. */
internal object FitKindText {
    fun note(strings: Strings, kinds: Map<BandRecordKind, Int>, duplicates: Int = 0): String? {
        val monitoring = kinds[BandRecordKind.MONITORING] ?: 0
        val activity = kinds[BandRecordKind.ACTIVITY] ?: 0
        val other = kinds[BandRecordKind.OTHER] ?: 0
        if (monitoring + activity + other == 0) return null
        val base = strings.get(R.string.hv_fit_kinds, monitoring, activity) +
            (if (other > 0) strings.get(R.string.hv_fit_kinds_other, other) else "")
        val kindsText = if (monitoring == 0 && activity > 0) base + "\n" + strings.get(R.string.hv_fit_activity_only) else base
        return if (duplicates > 0) kindsText + "\n" + strings.get(R.string.hv_fit_duplicates, duplicates) else kindsText
    }
}
