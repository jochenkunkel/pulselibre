package de.pulselibre

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class GarminHistoryTransportInspectorTest {
    @Test fun emptyAndUnregisteredPacketsRemainUnassigned() {
        val inspector = GarminHistoryTransportInspector()
        inspector.observe(byteArrayOf())
        inspector.observe(byteArrayOf(32, 1, 2))
        assertEquals(null, inspector.gfdiHandle)
        assertEquals(0, inspector.gfdiPackets)
        assertEquals(2, inspector.otherPackets)
    }

    @Test fun controlPacketIsNotMistakenForGfdiPayload() {
        val inspector = GarminHistoryTransportInspector()
        inspector.observe(byteArrayOf(0, 6))
        assertEquals(1, inspector.controlPackets)
        assertEquals(0, inspector.gfdiPackets)
        assertTrue(inspector.summary().contains("Steuerpakete: 1"))
    }
    @Test fun manyDistinctLengthsStayBoundedAndReportOverflow() {
        val inspector = GarminHistoryTransportInspector()
        for (length in 1..70) {
            inspector.observe(ByteArray(length) { 42 })
        }
        val summary = inspector.summary()
        assertTrue(summary.contains("1:1"))
        assertTrue(summary.contains("weitere Pakete: 6"))
        assertEquals(70, inspector.otherPackets)
    }

    @Test fun repeatedLengthIncrementsExistingBucketAfterOverflow() {
        val inspector = GarminHistoryTransportInspector()
        for (length in 1..65) inspector.observe(ByteArray(length) { 42 })
        inspector.observe(byteArrayOf(42))
        assertTrue(inspector.summary().contains("1:2"))
        assertTrue(inspector.summary().contains("weitere Pakete: 1"))
    }
}
