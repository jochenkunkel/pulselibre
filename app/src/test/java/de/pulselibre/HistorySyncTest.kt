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
    @Test fun movementStepHistoryKeepsChronologicalMeasurementsAfterPhoneAbsence() {
        val beforeDisconnect = HistoricalReading("cirqa", "steps", 1_000L, 1200.0, "first")
        val whileAway = listOf(
            HistoricalReading("cirqa", "steps", 3_000L, 1800.0, "third"),
            HistoricalReading("cirqa", "steps", 2_000L, 1500.0, "second")
        )
        val merged = mergeHistoricalReadings(listOf(beforeDisconnect), whileAway)
        assertEquals(listOf(1_000L, 2_000L, 3_000L), merged.map { it.measuredAtEpochMillis })
        assertEquals(listOf(1200.0, 1500.0, 1800.0), merged.map { it.value })
    }

    @Test fun backfillReplayAcrossReconnectsIsIdempotent() {
        val initial = listOf(sample(100, 95.0, "a"))
        val firstBackfill = listOf(sample(200, 96.0, "b"), sample(300, 97.0, "c"))
        val merged = mergeHistoricalReadings(initial, firstBackfill)
        assertEquals(merged, mergeHistoricalReadings(merged, firstBackfill.reversed()))
    }

    @Test fun failedOrEmptyBackfillNeverMovesCursor() {
        val cursor = SyncCursor(1_000L)
        assertEquals(cursor, commitSyncedBatch(cursor, emptyList()))
        assertEquals(cursor, commitSyncedBatch(cursor, listOf(sample(900L))))
        assertEquals(SyncCursor(1_200L), commitSyncedBatch(cursor, listOf(sample(1_200L))))
    }

    @Test fun differentDevicesAndMetricsMustNotBeDeduplicatedTogether() {
        val records = listOf(
            HistoricalReading("cirqa-1", "steps", 100L, 12.0),
            HistoricalReading("cirqa-1", "heart_rate", 100L, 70.0),
            HistoricalReading("cirqa-2", "steps", 100L, 15.0)
        )
        assertEquals(3, mergeHistoricalReadings(emptyList(), records).size)
    }

    @Test fun malformedHistoricalValuesAreRejectedWithoutDiscardingValidSamples() {
        val incoming = listOf(
            HistoricalReading("cirqa", "steps", 100L, 30.0),
            HistoricalReading("cirqa", "steps", 200L, Double.POSITIVE_INFINITY),
            HistoricalReading("cirqa", "steps", -1L, 40.0),
            HistoricalReading("", "steps", 300L, 50.0)
        )
        assertEquals(listOf(100L), mergeHistoricalReadings(emptyList(), incoming).map { it.measuredAtEpochMillis })
    }

}
