package de.pulselibre

/**
 * FIT-only projection for the science dashboard.
 *
 * The S10 test build must not pass untrusted FIT review candidates or unrelated
 * live/import rows into a verified band series. Until metric-specific validation
 * is agreed, only the existing provenance-backed heart-rate mapping is exposed.
 */
internal object ScienceFitSeriesAdapter {
    fun heartRate(
        observations: List<HistoricalObservation>,
        rangeStartMs: Long,
        rangeEndExclusiveMs: Long
    ): SeriesData {
        require(rangeStartMs >= 0 && rangeEndExclusiveMs > rangeStartMs)

        val points = observations.asSequence()
            .filter { observation ->
                observation.metric == FitHistoryMapping.METRIC &&
                    observation.unit == "bpm" &&
                    observation.provenance?.quality == FitHistoricalObservations.Quality.PROFILE_VALID.name &&
                    observation.provenance.timestampOrigin in setOf(
                        FitHistoricalObservations.TimestampOrigin.DIRECT.name,
                        FitHistoricalObservations.TimestampOrigin.RECONSTRUCTED_COMPRESSED.name,
                        FitHistoricalObservations.TimestampOrigin.RECONSTRUCTED_TS16.name
                    ) &&
                    observation.value.isFinite() &&
                    observation.value in 1.0..240.0 &&
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
            metricKey = "heart_rate",
            unit = "bpm",
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
}
