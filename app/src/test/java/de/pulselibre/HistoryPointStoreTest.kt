package de.pulselibre

import org.junit.Assert.*
import org.junit.Test
import java.time.Instant

class HistoryPointStoreTest {
    @Test fun growsAndSlicesInclusively() {
        val s = HistoryPointStore()
        for (i in 0 until 10_000) s.add(i * 1000L, 60 + i % 40)
        assertEquals(10_000, s.size)
        val slice = s.slice(2_000, 4_000)
        assertEquals(listOf(2_000L, 3_000L, 4_000L), slice.map { it.receivedAt.toEpochMilli() })
        assertEquals(62, slice.first().value)
    }
    @Test fun pruneKeepsRecentAndPreservesOrder() {
        val s = HistoryPointStore()
        (0 until 100).forEach { s.add(it * 10L, it) }
        s.pruneBefore(500)
        assertEquals(50, s.size)
        assertEquals(50, s.slice(0, 10_000).first().value)
    }
    @Test fun clearEmpties() {
        val s = HistoryPointStore(); s.add(1, 1); s.clear(); assertEquals(0, s.slice(0, 10).size)
    }
    @Test fun bucketsKeepMinMaxMedianAndMeanTogether() {
        val start = Instant.parse("2026-11-18T00:00:00Z")
        val pts = listOf(60, 70, 130).mapIndexed { i, v -> HistoryPoint(start.plusSeconds(i * 60L), v) }
        val b = aggregateHistoryPoints(pts, start, start.plusSeconds(3600), HistoryResolution.ONE_HOUR).single()
        assertEquals(60, b.minimum); assertEquals(130, b.maximum); assertEquals(70.0, b.median, 1e-9)
        assertEquals((60 + 70 + 130) / 3.0, b.mean, 1e-9); assertEquals(3, b.count)
    }
}

class HistoryPointStoreOrderTest {
    @Test fun sliceIsChronologicalEvenWhenInsertedOutOfOrder() {
        val s = HistoryPointStore()
        s.add(3000, 3); s.add(1000, 1); s.add(2000, 2); s.add(2000, 9)
        assertEquals(listOf(1, 2, 9, 3), s.slice(0, 5000).map { it.value })
    }
}

class HistoryRangeWindowTest {
    @Test fun offsetsStayInsideThirtyDays() {
        assertEquals(29, HistoryRange.DAY.maxOffset()); assertEquals(3, HistoryRange.WEEK.maxOffset()); assertEquals(0, HistoryRange.MONTH.maxOffset())
    }
    @Test fun windowEndMovesBackByWholeWindows() {
        val now = Instant.parse("2026-11-18T12:00:00Z")
        assertEquals(now, HistoryRange.DAY.windowEnd(now, 0))
        assertEquals(Instant.parse("2026-11-16T12:00:00Z"), HistoryRange.DAY.windowEnd(now, 2))
        assertEquals(Instant.parse("2026-11-04T12:00:00Z"), HistoryRange.WEEK.windowEnd(now, 2))
    }
}

class BucketStatsTest {
    @Test fun combiningUnequalBucketsUsesWeightedMeanNotMeanOfMeans() {
        val a = BucketStats.of(listOf(60, 60, 60, 60, 60, 60, 60, 60, 60))!!   // 9 samples, mean 60
        val b = BucketStats.of(listOf(100))!!                                  // 1 sample, mean 100
        val c = BucketStats.combine(listOf(a, b))!!
        assertEquals(64.0, c.mean, 1e-9)            // not (60+100)/2 = 80
        assertEquals(60L, c.minimum); assertEquals(100L, c.maximum); assertEquals(10, c.count); assertEquals(640L, c.sum)
    }
    @Test fun combiningIsAssociativeAndOrderIndependent() {
        val parts = listOf(listOf(70, 72), listOf(90), listOf(55, 56, 57)).map { BucketStats.of(it)!! }
        assertEquals(BucketStats.combine(parts), BucketStats.combine(parts.reversed()))
        assertEquals(BucketStats.of(listOf(70, 72, 90, 55, 56, 57)), BucketStats.combine(parts))
    }
    @Test fun emptyWindowsStayEmpty() {
        assertNull(BucketStats.of(emptyList())); assertNull(BucketStats.combine(emptyList()))
    }
    @Test fun meanOfAggregatedBucketMatchesTheBucketBuilder() {
        val start = Instant.parse("2026-11-18T00:00:00Z")
        val pts = listOf(60, 70, 130).mapIndexed { i, v -> HistoryPoint(start.plusSeconds(i * 60L), v) }
        val bucket = aggregateHistoryPoints(pts, start, start.plusSeconds(3600), HistoryResolution.ONE_HOUR).single()
        assertEquals(BucketStats.of(listOf(60, 70, 130))!!.mean, bucket.mean, 1e-9)
    }
}

class HistoryDatePickTest {
    private val today = java.time.LocalDate.of(2026, 11, 18)
    @Test fun dayRangeUsesWholeDaysAndClampsToTheCache() {
        assertEquals(0, HistoryRange.DAY.offsetForDate(today, today))
        assertEquals(5, HistoryRange.DAY.offsetForDate(today.minusDays(5), today))
        assertEquals(29, HistoryRange.DAY.offsetForDate(today.minusDays(200), today))
        assertEquals(0, HistoryRange.DAY.offsetForDate(today.plusDays(3), today))      // future never moves forward
    }
    @Test fun weekRangeUsesWholeWeeksAndMonthNeverMoves() {
        assertEquals(1, HistoryRange.WEEK.offsetForDate(today.minusDays(8), today))
        assertEquals(3, HistoryRange.WEEK.offsetForDate(today.minusDays(28), today))
        assertEquals(3, HistoryRange.WEEK.offsetForDate(today.minusDays(100), today))
        assertEquals(0, HistoryRange.MONTH.offsetForDate(today.minusDays(10), today))
    }
}
