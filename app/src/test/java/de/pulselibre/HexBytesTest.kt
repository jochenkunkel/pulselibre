package de.pulselibre

import org.junit.Assert.assertEquals
import org.junit.Test

class HexBytesTest {
    @Test fun matchesFormatForEveryByteValue() {
        val all = ByteArray(256) { it.toByte() }
        assertEquals(all.joinToString("") { "%02x".format(it.toInt() and 0xff) }, all.lowerHex())
        assertEquals(all.toList().joinToString("") { "%02x".format(it.toInt() and 0xff) }, all.toList().lowerHex())
    }

    @Test fun emptyIsEmpty() = assertEquals("", ByteArray(0).lowerHex())
}
