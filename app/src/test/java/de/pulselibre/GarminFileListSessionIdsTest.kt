package de.pulselibre

import org.junit.Assert.*
import org.junit.Test

class GarminFileListSessionIdsTest {
    @Test fun defaultCandidateMatchesExplicitLegacyIds() {
        assertArrayEquals(GarminFileListRequest.gfdi(), GarminFileListRequest.gfdi(10, 1))
    }

    @Test fun sessionIdsAreEncodedInCompactEnvelope() {
        val bytes = GarminFileListRequest.gfdi(11, 0x01ef)
        assertNotNull(GarminFrameValidator.inspectVerified(bytes))
        assertEquals(0x39, bytes[2].toInt() and 255)
        assertEquals(0x8b, bytes[3].toInt() and 255)
        assertEquals(0xef, bytes[4].toInt() and 255)
        assertEquals(0x01, bytes[5].toInt() and 255)
    }

    @Test fun invalidIdsAreRejectedBeforeEncoding() {
        try {
            GarminFileListRequest.gfdi(0, 1)
            fail("invalid sequence accepted")
        } catch (_: IllegalArgumentException) { }
        try {
            GarminFileListRequest.gfdi(10, 65536)
            fail("invalid counter accepted")
        } catch (_: IllegalArgumentException) { }
    }
}
