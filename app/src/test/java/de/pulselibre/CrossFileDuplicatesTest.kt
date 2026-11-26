package de.pulselibre

import java.time.Instant
import org.junit.Assert.assertEquals
import org.junit.Test

/** R1: the same measurement in two FIT files counts once; nothing else is merged. */
class CrossFileDuplicatesTest {
    private fun o(id: String, source: String?, value: Double = 60.0, timeMs: Long = 1_000L, unit: String = "bpm",
                  device: String = "band", origin: String = "DIRECT", message: Int = 55) = HistoricalObservation(
        deviceId = device, metric = "heart_rate", measuredAt = Instant.ofEpochMilli(timeMs), value = value, unit = unit,
        sampleId = id, receivedAt = null,
        provenance = source?.let {
            HistoricalObservation.Provenance(it.repeat(64), 1, message, 3, "PROFILE_VALID", FitHistoricalObservations.DECODER_VERSION, origin)
        })

    @Test fun keepsTheCopyOfTheSmallestFileAndAllNonDuplicates() {
        val rows = listOf(
            o("b1", "b"), o("a1", "a"),                                   // duplicate: keep file "a"
            o("a2", "a", value = 61.0), o("b2", "b", value = 62.0),       // other value
            o("a3", "a", timeMs = 2_000), o("b3", "b", timeMs = 2_000, unit = "count"),
            o("a4", "a", timeMs = 3_000), o("b4", "b", timeMs = 3_000, device = "other"),
            o("a5", "a", timeMs = 4_000, origin = "UNKNOWN"), o("b5", "b", timeMs = 4_000, origin = "UNKNOWN"),
            o("a6", "a", timeMs = 5_000), o("a7", "a", timeMs = 5_000),   // repeat inside one file
            o("n1", null, timeMs = 6_000), o("n2", null, timeMs = 6_000)  // no FIT provenance
        )
        assertEquals(rows.map { it.sampleId } - "b1", CrossFileDuplicates.countOnce(rows).map { it.sampleId })
    }

    @Test fun activityCopyInAnotherFileHidesTheMonitoringCopyLikeTheSqlRule() {
        val rows = listOf(o("m", "b", message = 55), o("act", "a", message = 20))
        val series = ScienceFitSeriesAdapter.verifiedSeries(ScienceFitSeriesAdapter.Metric.HEART_RATE, rows, 0, 10_000,
            BandRecordKind.MONITORING)
        assertEquals(0, series.points.size)
    }
}
