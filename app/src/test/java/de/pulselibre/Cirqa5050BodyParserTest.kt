package de.pulselibre

import org.junit.Assert.*
import org.junit.Test

class Cirqa5050BodyParserTest {
    @Test fun emptyBodyRejected() {
        assertNull(Cirqa5050BodyParser.parse(byteArrayOf()))
    }

    @Test fun zeroLengthBitmapAccepted() {
        val parsed = Cirqa5050BodyParser.parse(byteArrayOf(0))!!
        assertEquals(0, parsed.declaredBitmapBytes)
        assertArrayEquals(byteArrayOf(), parsed.bitmap)
        assertEquals(0, parsed.tailBytes)
    }

    @Test fun declaredBitmapIsExtractedWithoutTail() {
        val parsed = Cirqa5050BodyParser.parse(byteArrayOf(2, 8, 16, 99, 100))!!
        assertArrayEquals(byteArrayOf(8, 16), parsed.bitmap)
        assertEquals(2, parsed.tailBytes)
    }

    @Test fun truncatedBitmapRejected() {
        assertNull(Cirqa5050BodyParser.parse(byteArrayOf(3, 1, 2)))
    }

    @Test fun unsignedPrefixIsUsed() {
        assertNull(Cirqa5050BodyParser.parse(byteArrayOf(255.toByte(), 1)))
    }
}
