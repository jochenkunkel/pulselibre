package de.pulselibre

import org.junit.Assert.*
import org.junit.Test

class CirqaGfdiWriteTraceTest {
    @Test fun classifiesExistingRegistrationAndGfdiWritesWithoutPayloadBytes() {
        val trace = CirqaGfdiWriteTrace()
        trace.record(GarminMlHandshake.registerRequest(GarminMlHandshake.GFDI_SERVICE), "WITH_RESPONSE", 10)
        val response = CirqaOfflineGfdiStatusCandidate.ackFrame(5050)
        val framed = GarminFileListRequest.cobs(response)
        val mlValue = byteArrayOf(31) + framed
        trace.record(mlValue, "WITH_RESPONSE", 25)
        trace.completePendingCallback(android.bluetooth.BluetoothGatt.GATT_SUCCESS)

        val report = trace.summary()
        assertTrue(report.contains("ML-REGISTER(service=1)"))
        assertTrue(report.contains("GFDI-Typ=5000, CRC=OK"))
        assertTrue(report.contains("GATT-callback=SUCCESS"))
        assertFalse(report.contains(response.joinToString("") { "%02x".format(it.toInt() and 255) }))
        assertFalse(report.contains("5050-Antwort")) // No body semantics/raw content in TX trace.
    }

    @Test fun traceIsHardBoundedAndClampsNegativeRelativeTime() {
        val trace = CirqaGfdiWriteTrace(maxEvents = 2)
        repeat(3) { trace.record(byteArrayOf(), "WITH_RESPONSE", -5) }
        val report = trace.summary()
        assertTrue(report.contains("2 erfasst, 1 wegen Limit ausgelassen"))
        assertTrue(report.contains("TX +0ms"))
        assertEquals(2, Regex("TX \\+").findAll(report).count())
    }
}
