package de.pulselibre

import org.junit.Assert.*
import org.junit.Test

class HistorySyncTest {
    private fun sample(time: Long, value: Double = 97.0, id: String? = null) =
        HistoricalReading("band-1", "spo2", time, value, id)

    @Test fun offlineBackfillIsChronological() {
        val merged = mergeHistoricalReadings(listOf(sample(300)), listOf(sample(100), sample(200)))
        assertEquals(listOf(100L, 200L, 300L), merged.map { it.measuredAtEpochMillis })
    }
    @Test fun reconnectReplayDoesNotDuplicate() {
        val once = mergeHistoricalReadings(emptyList(), listOf(sample(100), sample(200)))
        assertEquals(once, mergeHistoricalReadings(once, listOf(sample(100), sample(200))))
    }
    @Test fun distinctSamplesAtSameTimestampArePreserved() {
        assertEquals(2, mergeHistoricalReadings(emptyList(),
            listOf(sample(100, 96.0, "a"), sample(100, 97.0, "b"))).size)
    }
    @Test fun invalidDataIsNotImported() {
        val bad = sample(10, Double.NaN)
        assertTrue(mergeHistoricalReadings(emptyList(), listOf(bad, sample(-1))).isEmpty())
    }
    @Test fun emptyOrInterruptedBatchDoesNotAdvanceCursor() {
        val cursor = SyncCursor(200)
        assertEquals(cursor, commitSyncedBatch(cursor, emptyList()))
        assertEquals(cursor, commitSyncedBatch(cursor, listOf(sample(100))))
        assertEquals(SyncCursor(300), commitSyncedBatch(cursor, listOf(sample(300))))
    }
}
