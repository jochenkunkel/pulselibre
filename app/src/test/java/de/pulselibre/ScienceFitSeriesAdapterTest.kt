package de.pulselibre

import java.time.Instant
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ScienceFitSeriesAdapterTest {
    private fun observation(
        metric: String = "heart_rate",
        unit: String = "bpm",
        value: Double = 72.0,
        timeMs: Long = 1_000L,
        quality: String = "PROFILE_VALID",
        timestampOrigin: String = "DIRECT",
        withProvenance: Boolean = true
    ) = HistoricalObservation(
        deviceId = "band",
        metric = metric,
        measuredAt = Instant.ofEpochMilli(timeMs),
        value = value,
        unit = unit,
        sampleId = "sample-$timeMs-$metric",
        receivedAt = null,
        provenance = if (withProvenance) HistoricalObservation.Provenance(
            sourceFileSha256 = "a".repeat(64),
            recordOrdinal = 1,
            globalMessageNumber = 20,
            fieldNumber = 3,
            quality = quality,
            decoderVersion = FitHistoricalObservations.DECODER_VERSION,
            timestampOrigin = timestampOrigin
        ) else null
    )

    @Test fun projectsOnlyProvenanceBackedInRangeProfileValidFitHeartRate() {
        val start = 1_000L
        val endExclusive = 5_000L
        val series = ScienceFitSeriesAdapter.heartRate(
            listOf(
                observation(timeMs = 2_000L),
                observation(timeMs = 3_000L, timestampOrigin = "RECONSTRUCTED_TS16"),
                observation(metric = "distance", unit = "m", value = 10.0, timeMs = 2_500L),
                observation(timeMs = 1_500L, withProvenance = false),
                observation(timeMs = 1_600L, quality = "OUTSIDE_DISPLAY_RANGE"),
                observation(timeMs = 1_700L, timestampOrigin = "UNKNOWN"),
                observation(timeMs = 1_800L, timestampOrigin = "RECONSTRUCTED_COMPRESSED"),
                observation(timeMs = 4_000L, value = 0.0),
                observation(timeMs = 4_100L, value = 241.0),
                observation(timeMs = start - 1),
                observation(timeMs = endExclusive)
            ),
            rangeStartMs = start,
            rangeEndExclusiveMs = endExclusive
        )

        assertEquals("heart_rate", series.metricKey)
        assertEquals("bpm", series.unit)
        assertEquals(SeriesOrigin.BAND, series.origin)
        assertEquals(SeriesTimeBasis.BAND_CLOCK, series.timeBasis)
        assertEquals(start, series.rangeStartMs)
        assertEquals(endExclusive - 1, series.rangeEndMs)
        assertEquals(null, series.nominalIntervalMs)
        assertEquals(listOf(2_000L, 3_000L), series.points.map { it.timeMs })
        assertTrue(series.points.all { it.quality == SeriesQuality.VALID })
    }

    @Test fun returnsAnEmptyBandSeriesWhenNoVerifiedFitHeartRateExists() {
        val series = ScienceFitSeriesAdapter.heartRate(emptyList(), 1_000L, 2_000L)
        assertTrue(series.points.isEmpty())
        assertEquals(SeriesOrigin.BAND, series.origin)
        assertEquals(SeriesTimeBasis.BAND_CLOCK, series.timeBasis)
    }

    @Test fun projectsOnlyTheAlreadyStoredCanonicalFitMetricsAndTheirUnits() {
        val source = listOf(
            observation(metric = "distance", unit = "m", value = 12.5, timeMs = 1_100L),
            observation(metric = "speed", unit = "m/s", value = 1.25, timeMs = 1_200L),
            observation(metric = "cadence", unit = "rpm", value = 70.0, timeMs = 1_300L),
            observation(metric = "power", unit = "W", value = 80.0, timeMs = 1_400L)
        )

        val distance = ScienceFitSeriesAdapter.verifiedSeries(
            ScienceFitSeriesAdapter.Metric.DISTANCE, source, 1_000L, 2_000L
        )
        val speed = ScienceFitSeriesAdapter.verifiedSeries(
            ScienceFitSeriesAdapter.Metric.SPEED, source, 1_000L, 2_000L
        )

        assertEquals("m", distance.unit)
        assertEquals(listOf(12.5), distance.points.map { it.value })
        assertEquals("m/s", speed.unit)
        assertEquals(listOf(1.25), speed.points.map { it.value })
    }
}
