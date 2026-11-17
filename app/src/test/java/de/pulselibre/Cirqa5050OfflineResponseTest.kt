package de.pulselibre

import org.junit.Assert.*
import org.junit.Test

class Cirqa5050OfflineResponseTest {
    @Test fun encodesExplicitCapabilitiesWithLengthAndCrc() {
        val frame = Cirqa5050OfflineResponse.frame(byteArrayOf(1, 0, 2))
        assertEquals(10, frame.size)
        assertNotNull(GarminFrameValidator.inspectVerified(frame))
        val shape = Cirqa5050OfflineCapabilities.inspect(frame)!!
        assertTrue(shape.lengthMatches)
        assertEquals(3, shape.declaredLength)
        assertEquals(2, shape.setBits)
    }

    @Test fun refusesImplicitOrEmptyCapabilities() {
        try {
            Cirqa5050OfflineResponse.frame(byteArrayOf())
            fail("Expected explicit capabilities")
        } catch (_: IllegalArgumentException) { }
    }

    @Test fun neverMutatesCallerCapabilities() {
        val capabilities = byteArrayOf(4, 8)
        Cirqa5050OfflineResponse.frame(capabilities)
        assertArrayEquals(byteArrayOf(4, 8), capabilities)
    }
}
