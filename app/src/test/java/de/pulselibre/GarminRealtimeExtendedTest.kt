package de.pulselibre

import org.junit.Assert.*
import org.junit.Test

class GarminRealtimeExtendedTest {
    @Test fun oxygenAndMissingSensorSentinel() {
        assertEquals(GarminRealtime.Reading.SpO2(97), GarminRealtime.decode(19, byteArrayOf(97)))
        assertNull(GarminRealtime.decode(19, byteArrayOf(-1)))
        assertNull(GarminRealtime.decode(19, byteArrayOf()))
        assertNull(GarminRealtime.decode(19, byteArrayOf(0)))
    }

    @Test fun respirationAndNegativeUnavailableValues() {
        assertEquals(GarminRealtime.Reading.Respiration(16), GarminRealtime.decode(21, byteArrayOf(16)))
        assertNull(GarminRealtime.decode(21, byteArrayOf(-1)))
        assertNull(GarminRealtime.decode(13, byteArrayOf(-1)))
        assertNull(GarminRealtime.decode(20, byteArrayOf(-1)))
    }

    @Test fun hrvStressAndBodyBattery() {
        assertEquals(GarminRealtime.Reading.Hrv(850), GarminRealtime.decode(12, byteArrayOf(0x52, 3)))
        assertEquals(GarminRealtime.Reading.Stress(34), GarminRealtime.decode(13, byteArrayOf(34)))
        assertEquals(GarminRealtime.Reading.BodyBattery(85), GarminRealtime.decode(20, byteArrayOf(85)))
        assertNull(GarminRealtime.decode(12, byteArrayOf(0)))
    }

    @Test fun caloriesAndIntensityLittleEndian() {
        assertEquals(GarminRealtime.Reading.Calories(1200, 400),
            GarminRealtime.decode(8, byteArrayOf(-80, 4, 0, 0, -112, 1, 0, 0)))
        assertEquals(GarminRealtime.Reading.Intensity(30, 15),
            GarminRealtime.decode(10, byteArrayOf(30, 0, 15, 0)))
        assertNull(GarminRealtime.decode(8, ByteArray(7)))
        assertNull(GarminRealtime.decode(10, ByteArray(3)))
    }

    @Test fun unknownHandleCannotProduceOxygenReading() {
        val registry = GarminRealtime.Registry()
        assertNull(registry.receive(byteArrayOf(0xb0.toByte(), 97)))
        assertTrue(registry.register(19, 3))
        assertEquals(GarminRealtime.Reading.SpO2(97), registry.receive(byteArrayOf(0xb0.toByte(), 97)))
        registry.clear()
        assertNull(registry.receive(byteArrayOf(0xb0.toByte(), 97)))
    }
}
