package de.pulselibre

import org.junit.Assert.*
import org.junit.Test

class GarminRealtimeTest {
    @Test fun parsesHeartRateAndUnknownRestingValue() {
        assertEquals(GarminRealtime.Reading.HeartRate(72, null),
            GarminRealtime.decode(6, byteArrayOf(3, 72, 0)))
        assertEquals(GarminRealtime.Reading.HeartRate(72, 54),
            GarminRealtime.decode(6, byteArrayOf(3, 72, 54)))
        assertNull(GarminRealtime.decode(6, byteArrayOf(3, 0, 0)))
        assertNull(GarminRealtime.decode(6, byteArrayOf(3, 72)))
    }

    @Test fun parsesUnsignedLittleEndianSteps() {
        val payload = byteArrayOf(0x78, 0x56, 0x34, 0x12, 0x10, 0x27, 0, 0)
        assertEquals(GarminRealtime.Reading.Steps(0x12345678L, 10000L),
            GarminRealtime.decode(7, payload))
        assertEquals(GarminRealtime.Reading.Steps(4294967295L, 0L),
            GarminRealtime.decode(7, byteArrayOf(-1, -1, -1, -1, 0, 0, 0, 0)))
        assertNull(GarminRealtime.decode(7, ByteArray(7)))
    }

    @Test fun handlesBothRoutingFormatsAndRejectsControl() {
        assertEquals(3, GarminRealtime.route(byteArrayOf(0xb0.toByte(), 3, 72, 54))?.handle)
        assertEquals(9, GarminRealtime.route(byteArrayOf(9, 1))?.handle)
        assertNull(GarminRealtime.route(byteArrayOf(0, 1, 2)))
        assertNull(GarminRealtime.route(byteArrayOf(0x80.toByte(), 1)))
        assertNull(GarminRealtime.route(byteArrayOf(3)))
    }

    @Test fun requiresRegisteredHandleAndNeverGuessesService() {
        val registry = GarminRealtime.Registry()
        val packet = byteArrayOf(0xb0.toByte(), 3, 72, 54)
        assertNull(registry.receive(packet))
        assertFalse(registry.register(6, 0))
        assertTrue(registry.register(6, 3))
        assertEquals(GarminRealtime.Reading.HeartRate(72, 54), registry.receive(packet))
        assertNull(registry.receive(byteArrayOf(0xc0.toByte(), 3, 72, 54)))
        registry.clear()
        assertNull(registry.receive(packet))
        assertNull(GarminRealtime.decode(1, byteArrayOf(3, 72, 54)))
    }
}
