package de.pulselibre

import java.time.Instant

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
        rangeEndExclusiveMs: Long,
        /** Only values of this FIT message kind (normal all-day history or activity); null = all kinds. */
        kind: BandRecordKind? = null
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
            .toList()
            // Same measurement in two FIT files: shown once (R1), decided before the kind split like the SQL rule.
            .let(CrossFileDuplicates::countOnce)
            .filter { kind == null || BandRecordKind.of(it.provenance!!.globalMessageNumber) == kind }
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

    /**
     * Reads all pages from the provenance-bearing repository before projecting.
     * A single repository page is capped at 5000 rows and must never be mistaken
     * for the complete selected time range.
     */
    fun readVerifiedSeries(
        repository: HistoricalObservationRepository,
        metric: Metric,
        rangeStartMs: Long,
        rangeEndExclusiveMs: Long,
        pageSize: Int = 5_000,
        kind: BandRecordKind? = null
    ): SeriesData = verifiedSeries(metric, readVerifiedObservations(repository, metric, rangeStartMs, rangeEndExclusiveMs, pageSize),
        rangeStartMs, rangeEndExclusiveMs, kind)

    /** All stored observations of [metric] in the range (every page), for several projections from one read. */
    fun readVerifiedObservations(
        repository: HistoricalObservationRepository,
        metric: Metric,
        rangeStartMs: Long,
        rangeEndExclusiveMs: Long,
        pageSize: Int = 5_000
    ): List<HistoricalObservation> {
        require(rangeStartMs >= 0 && rangeEndExclusiveMs > rangeStartMs)
        require(pageSize in 1..5_000)
        return readAllPages { cursor ->
            repository.read(
                HistoricalObservationQuery(
                    startInclusive = Instant.ofEpochMilli(rangeStartMs),
                    endExclusive = Instant.ofEpochMilli(rangeEndExclusiveMs),
                    metric = metric.key,
                    limit = pageSize,
                    after = cursor
                )
            )
        }
    }

    internal fun readAllPages(
        readPage: (HistoricalObservationQuery.Cursor?) -> HistoricalObservationPage
    ): List<HistoricalObservation> {
        val all = ArrayList<HistoricalObservation>()
        var cursor: HistoricalObservationQuery.Cursor? = null
        var pages = 0
        while (true) {
            check(pages++ < 10_000) { "Historical observation pagination exceeded safety limit" }
            val page = readPage(cursor)
            all.addAll(page.observations)
            val next = page.next ?: return all
            check(next != cursor) { "Historical observation pagination cursor did not advance" }
            cursor = next
        }
    }
}
