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
        withProvenance: Boolean = true,
        globalMessage: Int = 20
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
            globalMessageNumber = globalMessage,
            fieldNumber = 3,
            quality = quality,
            decoderVersion = FitHistoricalObservations.DECODER_VERSION,
            timestampOrigin = timestampOrigin
        ) else null
    )

    @Test fun allDayHistoryAndActivityAreSeparateSeries() {
        val rows = listOf(
            observation(timeMs = 2_000L, value = 61.0, globalMessage = 55),  // monitoring: all-day history
            observation(timeMs = 2_100L, value = 62.0, globalMessage = 55),
            observation(timeMs = 3_000L, value = 140.0, globalMessage = 20), // record: activity
            observation(timeMs = 3_100L, value = 70.0, globalMessage = 18)   // other message: in neither card
        )
        val allDay = ScienceFitSeriesAdapter.verifiedSeries(ScienceFitSeriesAdapter.Metric.HEART_RATE, rows, 1_000L, 5_000L, BandRecordKind.MONITORING)
        val activity = ScienceFitSeriesAdapter.verifiedSeries(ScienceFitSeriesAdapter.Metric.HEART_RATE, rows, 1_000L, 5_000L, BandRecordKind.ACTIVITY)
        assertEquals(listOf(61.0, 62.0), allDay.points.map { it.value })
        assertEquals(listOf(140.0), activity.points.map { it.value })
        // Without a kind (old callers) every valid value stays in one series.
        assertEquals(4, ScienceFitSeriesAdapter.heartRate(rows, 1_000L, 5_000L).points.size)
    }

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

    @Test fun readsEveryRepositoryPageAndPassesTheCursorForward() {
        val first = observation(timeMs = 1_100L)
        val second = observation(timeMs = 1_200L)
        val cursor = HistoricalObservationQuery.Cursor(1_100L, "band", "heart_rate", first.sampleId)
        val seen = mutableListOf<HistoricalObservationQuery.Cursor?>()

        val result = ScienceFitSeriesAdapter.readAllPages { after ->
            seen += after
            if (after == null) HistoricalObservationPage(listOf(first), cursor)
            else HistoricalObservationPage(listOf(second), null)
        }

        assertEquals(listOf(null, cursor), seen)
        assertEquals(listOf(first, second), result)
    }
}
