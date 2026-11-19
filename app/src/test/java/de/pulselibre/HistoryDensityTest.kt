package de.pulselibre

import java.time.Instant
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class HistoryDensityTest {
    private val start = Instant.parse("2026-11-19T00:00:00Z")
    private val end = start.plusSeconds(3600)
    private val axis = AxisScale(60, 100)

    @Test fun histogramUsesRawSamplesAndFindsModeIndependentOfMean() {
        val points = listOf(70, 70, 70, 70, 71, 72, 90).map {
            HistoryPoint(start.plusSeconds(1), it)
        }

        val column = density(points).columns.first()

        assertEquals(7, column.sampleCount)
        assertEquals(5, column.bins[5].count) // [70, 72): 70 and 71
        assertEquals(1, column.bins[6].count) // 72 is on the next bin's lower edge
        assertEquals(1, column.bins[15].count) // 90
        assertTrue(column.bins[6].count < column.bins[5].count) // mean does not become the density peak
    }

    @Test fun bimodalDistributionKeepsTwoPeaksAndDoesNotInventMiddlePeak() {
        val points = (listOf(60, 60, 60, 100, 100, 100)).map {
            HistoryPoint(start.plusSeconds(10), it)
        }

        val column = density(points).columns.first()

        assertEquals(3, column.bins.first().count)
        assertEquals(3, column.bins.last().count)
        assertEquals(0, column.bins[10].count)
    }

    @Test fun slotsIncludeEmptyWindowsAndNeverBridgeThem() {
        val points = listOf(
            HistoryPoint(start.plusSeconds(10), 70),
            HistoryPoint(start.plusSeconds(30 * 60L + 10), 80)
        )

        val result = aggregateHistoryDensity(
            points, start, end, HistoryResolution.TWO_HOURS, axis, valueBinWidth = 2
        )

        assertEquals(1, result.columns.size)
        assertEquals(2, result.columns.single().sampleCount)
        val quarterHourly = aggregateHistoryDensity(
            points, start, end, HistoryResolution.FIFTEEN_MINUTES, axis, valueBinWidth = 2
        )
        assertEquals(4, quarterHourly.columns.size)
        assertEquals(1, quarterHourly.columns[0].sampleCount)
        assertEquals(0, quarterHourly.columns[1].sampleCount)
        assertEquals(1, quarterHourly.columns[2].sampleCount)
        assertEquals(0, quarterHourly.columns[3].sampleCount)
        assertTrue(quarterHourly.columns[1].bins.all { it.probability == 0.0 && it.intensity == 0.0 })
    }

    @Test fun valueBinsAreHalfOpenExceptTheInclusiveAxisMaximum() {
        val points = listOf(60, 61, 62, 99, 100, 59, 101).map {
            HistoryPoint(start.plusSeconds(1), it)
        }

        val column = density(points).columns.single()

        assertEquals(5, column.sampleCount)
        assertEquals(2, column.excludedOutsideAxis)
        assertEquals(2, column.bins[0].count) // 60, 61
        assertEquals(1, column.bins[1].count) // 62
        assertEquals(2, column.bins.last().count) // 99, inclusive axis maximum 100
        assertEquals(60, column.bins.first().lowerInclusive)
        assertEquals(62, column.bins.first().upperBound)
        assertFalse(column.bins.first().upperBoundInclusive)
        assertEquals(100, column.bins.last().upperBound)
        assertTrue(column.bins.last().upperBoundInclusive)
    }

    @Test fun sampleAtRangeEndFallsIntoFinalSlot() {
        val points = listOf(HistoryPoint(end, 75))
        val result = aggregateHistoryDensity(
            points, start, end, HistoryResolution.FIFTEEN_MINUTES, axis, valueBinWidth = 2
        )

        assertEquals(4, result.columns.size)
        assertEquals(0, result.columns[2].sampleCount)
        assertEquals(1, result.columns[3].sampleCount)
        assertEquals(end, result.columns[3].end)
    }

    @Test fun sharedReferenceDoesNotNormalizeEachColumnToItsOwnMaximum() {
        val points = buildList {
            repeat(8) { add(HistoryPoint(start.plusSeconds(10), 70)) }
            repeat(2) { add(HistoryPoint(start.plusSeconds(10), 72)) }
            repeat(2) { add(HistoryPoint(start.plusSeconds(1900), 80)) }
            repeat(2) { add(HistoryPoint(start.plusSeconds(1900), 82)) }
        }

        val result = aggregateHistoryDensity(
            points, start, end, HistoryResolution.FIFTEEN_MINUTES, axis, valueBinWidth = 2
        )
        val concentrated = result.columns[0].bins[5].intensity
        val diffuse = result.columns[0].bins[6].intensity
        val laterColumnPeak = result.columns[2].bins[10].intensity

        assertEquals(1.0, concentrated, 0.0)
        assertTrue(diffuse < concentrated)
        assertTrue(laterColumnPeak < 1.0) // column is not independently stretched to full intensity
        assertTrue(result.referenceProbability > 0.0)
    }

    @Test fun allSlotsRemainTransparentWhenNoValuesFitTheAxis() {
        val result = density(listOf(HistoryPoint(start.plusSeconds(1), 120)))

        assertEquals(0.0, result.referenceProbability, 0.0)
        assertEquals(0, result.columns.single().sampleCount)
        assertEquals(1, result.columns.single().excludedOutsideAxis)
        assertTrue(result.columns.single().bins.all { it.count == 0 && it.intensity == 0.0 })
    }

    @Test(expected = IllegalArgumentException::class)
    fun excessiveSlotCountIsRejected() {
        aggregateHistoryDensity(
            emptyList(), start, start.plusSeconds(4097L * 900L),
            HistoryResolution.FIFTEEN_MINUTES, axis, valueBinWidth = 2
        )
    }

    private fun density(points: List<HistoryPoint>) =
        aggregateHistoryDensity(
            points, start, end, HistoryResolution.TWO_HOURS, axis, valueBinWidth = 2
        )
}
