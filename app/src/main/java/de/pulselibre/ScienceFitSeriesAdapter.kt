package de.pulselibre

/**
 * FIT-only projection for the science dashboard.
 *
 * The S10 test build must not pass untrusted FIT review candidates or unrelated
 * live/import rows into a verified band series. Only FIT metrics already present
 * in the canonical production store are exposed.
 */
internal object ScienceFitSeriesAdapter {
    enum class Metric(val key: String, val unit: String) {
        HEART_RATE("heart_rate", "bpm"),
        DISTANCE("distance", "m"),
        SPEED("speed", "m/s")
    }

    fun verifiedSeries(
        metric: Metric,
        observations: List<HistoricalObservation>,
        rangeStartMs: Long,
        rangeEndExclusiveMs: Long
    ): SeriesData {
        require(rangeStartMs >= 0 && rangeEndExclusiveMs > rangeStartMs)

        val points = observations.asSequence()
            .filter { observation ->
                observation.metric == metric.key &&
                    observation.unit == metric.unit &&
                    observation.provenance?.quality == FitHistoricalObservations.Quality.PROFILE_VALID.name &&
                    observation.provenance.timestampOrigin in setOf(
                        FitHistoricalObservations.TimestampOrigin.DIRECT.name,
                        FitHistoricalObservations.TimestampOrigin.RECONSTRUCTED_TS16.name
                    ) &&
                    observation.value.isFinite() &&
                    (metric != Metric.HEART_RATE || observation.value in 1.0..240.0) &&
                    observation.value >= 0.0 &&
                    observation.measuredAt.toEpochMilli() >= rangeStartMs &&
                    observation.measuredAt.toEpochMilli() < rangeEndExclusiveMs
            }
            .sortedBy { it.measuredAt }
            .map { observation ->
                SeriesPoint(
                    timeMs = observation.measuredAt.toEpochMilli(),
                    value = observation.value,
                    quality = SeriesQuality.VALID
                )
            }
            .toList()

        return SeriesData(
            metricKey = metric.key,
            unit = metric.unit,
            origin = SeriesOrigin.BAND,
            timeBasis = SeriesTimeBasis.BAND_CLOCK,
            rangeStartMs = rangeStartMs,
            // ScienceModel uses an inclusive range end, while the repository uses [start, end).
            rangeEndMs = rangeEndExclusiveMs - 1,
            // No nominal sample interval is established by the FIT profile.
            nominalIntervalMs = null,
            points = points
        )
    }

    fun heartRate(
        observations: List<HistoricalObservation>,
        rangeStartMs: Long,
        rangeEndExclusiveMs: Long
    ): SeriesData = verifiedSeries(Metric.HEART_RATE, observations, rangeStartMs, rangeEndExclusiveMs)
}
