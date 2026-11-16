package de.pulselibre

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class LocalReadingStoreTest {
    private val context: Context = ApplicationProvider.getApplicationContext()

    @Test fun batchIsDurableIdempotentAndCursorMonotonic() {
        context.deleteDatabase("pulselibre_readings.db")
        val reading = RawExportRecord("test-device", "spo2", 2000, 97.0, "%", "a", 3000)
        LocalReadingStore(context).use { store ->
            store.commitBatch("test-device", listOf(reading))
            store.commitBatch("test-device", listOf(reading))
            assertEquals(1, store.readWindow(ExportWindow(0, 3000)).size)
            assertEquals(2000L, store.cursorFor("test-device").lastCommittedEpochMillis)
        }
        LocalReadingStore(context).use { store ->
            assertEquals(1, store.readWindow(ExportWindow(0, 3000)).size)
            store.commitBatch("test-device", listOf(reading.copy(measuredAtEpochMillis = 1000, sampleId = "b")))
            assertEquals(2000L, store.cursorFor("test-device").lastCommittedEpochMillis)
            assertEquals(2, store.readWindow(ExportWindow(0, 3000)).size)
            assertEquals(0, store.readWindow(ExportWindow(3000, 4000)).size)
        }
    }

    @Test fun invalidBatchDoesNotWriteOrAdvanceCursor() {
        context.deleteDatabase("pulselibre_readings.db")
        LocalReadingStore(context).use { store ->
            try {
                store.commitBatch("test-device", listOf(
                    RawExportRecord("test-device", "hr", 100, 75.0, "bpm"),
                    RawExportRecord("wrong-device", "hr", 200, 80.0, "bpm")
                ))
                fail("Expected validation failure")
            } catch (_: IllegalArgumentException) { }
            assertTrue(store.readWindow(ExportWindow(0, 300)).isEmpty())
            assertNull(store.cursorFor("test-device").lastCommittedEpochMillis)
        }
    }
}
