package de.pulselibre

import org.junit.Assert.*
import org.junit.Test

class Cirqa5050OfflineHostCapabilitiesTest {
    @Test fun defaultAdvertisesNoUnverifiedCapabilities() {
        for (size in listOf(15, 18)) {
            val bytes = Cirqa5050OfflineHostCapabilities.candidate(byteCount = size)
            assertEquals(size, bytes.size)
            assertTrue(bytes.all { it == 0.toByte() })
            val shape = Cirqa5050OfflineCapabilities.inspect(
                Cirqa5050OfflineHostCapabilities.offlineFrame(byteCount = size))!!
            assertTrue(shape.lengthMatches)
            assertEquals(size, shape.declaredLength)
            assertEquals(0, shape.setBits)
        }
    }

    @Test fun syncRequiresExplicitOfflineOptInAndSetsOnlyBitThree() {
        val bytes = Cirqa5050OfflineHostCapabilities.candidate(true, 18)
        assertEquals(8, bytes[0].toInt() and 255)
        assertTrue(bytes.drop(1).all { it == 0.toByte() })
        val shape = Cirqa5050OfflineCapabilities.inspect(
            Cirqa5050OfflineHostCapabilities.offlineFrame(true, 18))!!
        assertEquals(1, shape.setBits)
    }

    @Test fun rejectsArbitraryHostMaskLength() {
        try {
            Cirqa5050OfflineHostCapabilities.candidate(byteCount = 19)
            fail("Unexpected size accepted")
        } catch (_: IllegalArgumentException) { }
    }
}
