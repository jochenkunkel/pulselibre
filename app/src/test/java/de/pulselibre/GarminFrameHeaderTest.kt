package de.pulselibre

import org.junit.Assert.*
import org.junit.Test

class GarminFrameHeaderTest {
    @Test fun identifiesKnownTypeInLittleEndianHeader() {
        val header = GarminFrameHeader.inspect(byteArrayOf(6, 0, 0xB3.toByte(), 0x13, 0, 0))
        assertEquals(5043, header?.messageType)
        assertEquals("PROTOBUF_REQUEST", header?.knownType)
    }

    @Test fun rejectsIncompleteAndLengthMismatch() {
        assertNull(GarminFrameHeader.inspect(byteArrayOf(5, 0, 0, 0)))
        assertNull(GarminFrameHeader.inspect(byteArrayOf(7, 0, 0, 0, 0, 0)))
    }

    @Test fun unknownTypeDoesNotBecomeHealthData() {
        assertEquals("UNKNOWN", GarminFrameHeader.inspect(byteArrayOf(6, 0, 1, 0, 0, 0))?.knownType)
    }
}
