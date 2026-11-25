package de.pulselibre

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * A second GATT client beside a running history download subscribes the same ML channel, registers GFDI again and
 * the download takes that foreign registration response for its own handle. Guard both the start policy of the
 * device page and the rule that every writing BLE client takes [CirqaBleSessionGate].
 */
class BleSessionExclusionTest {
    private val src = listOf(File("src/main/java/de/pulselibre"), File("app/src/main/java/de/pulselibre")).first { it.isDirectory }

    @Test fun metadataProbeNeverStartsBesideAnotherSession() {
        assertTrue(DeviceMetadataStartPolicy.mayStart(bleSessionBusy = false, historySyncActive = false))
        assertFalse(DeviceMetadataStartPolicy.mayStart(bleSessionBusy = true, historySyncActive = false))
        assertFalse(DeviceMetadataStartPolicy.mayStart(bleSessionBusy = false, historySyncActive = true))
        assertFalse(DeviceMetadataStartPolicy.mayStart(bleSessionBusy = true, historySyncActive = true))
    }

    @Test fun everyGattClientThatWritesTakesTheSessionGate() {
        val sources = src.listFiles().orEmpty().filter { it.extension == "kt" }
        // Discovery-only client of the test laboratory: reads services, never writes to the band.
        val readOnly = setOf("CirqaGattProbe.kt")
        val clients = sources.filter { "connectGatt(" in it.readText() }.map { it.name }.sorted()
        assertTrue("no GATT client found: wrong working directory?", clients.isNotEmpty())
        val withoutGate = clients.filter { it !in readOnly && "CirqaBleSessionGate.acquire(" !in File(src, it).readText() }
        assertEquals("GATT clients without CirqaBleSessionGate.acquire", emptyList<String>(), withoutGate)
    }

    @Test fun probeAndMetadataReleaseTheGateOnEveryEnd() {
        val metadata = File(src, "DeviceMetadataProbe.kt").readText()
        // finish() (both branches) and close() release; a leaked owner would block every later download.
        assertTrue(Regex("CirqaBleSessionGate\\.release\\(this\\)").findAll(metadata).count() >= 3)
    }
}
