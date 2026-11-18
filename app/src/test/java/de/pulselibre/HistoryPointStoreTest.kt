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
