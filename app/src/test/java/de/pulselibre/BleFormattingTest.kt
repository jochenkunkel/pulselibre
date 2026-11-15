package de.pulselibre

import org.junit.Assert.assertEquals
import org.junit.Test

class BleFormattingTest {
    @Test fun bytesAreUnsignedHex() {
        assertEquals("00 7F 80 FF", byteArrayOf(0, 127, -128, -1).toHex())
    }
    @Test fun emptyPayloadIsEmptyString() {
        assertEquals("", byteArrayOf().toHex())
    }
}
