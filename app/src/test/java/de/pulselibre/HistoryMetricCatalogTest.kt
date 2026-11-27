package de.pulselibre

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class HistoryMetricCatalogTest {
    @Test fun liveAndBandMetricsOfOneMeasureShareOneSeries() {
        val series = HistoryMetricCatalog.group(listOf(
            Triple("heart_rate", 2_000L, 61.0), Triple("bpm", 1_000L, 80.0),
            Triple("spo2", 3_000L, 97.0), Triple("spo2_percent", 4_000L, 95.0),
            Triple("respiration", 5_000L, 14.6), Triple("breaths_per_min", 6_000L, 15.0)))
        assertEquals(listOf(80, 61), series["heart_rate"]!!.map { it.value })   // sorted by time
        assertEquals(2, series["spo2"]!!.size)
        assertEquals(listOf(15, 15), series["respiration"]!!.map { it.value })
    }

    @Test fun implausibleValuesAndForeignMetricsAreDropped() {
        val series = HistoryMetricCatalog.group(listOf(
            Triple("heart_rate", 1L, 0.0), Triple("heart_rate", 2L, 255.0), Triple("stress", 3L, -1.0),
            Triple("active_calories", 4L, 500.0), Triple("steps", 5L, 100.0)))
        assertNull(series["heart_rate"]); assertNull(series["stress"]); assertEquals(0, series.size)
    }

    @Test fun everyMetricBelongsToExactlyOneSpec() {
        val all = HistoryMetricCatalog.SPECS.flatMap { it.metrics }
        assertEquals(all.size, all.toSet().size)
    }
}
