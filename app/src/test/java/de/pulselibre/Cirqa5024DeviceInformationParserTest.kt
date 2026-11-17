package de.pulselibre

import org.junit.Assert.*
import org.junit.Test

class Cirqa5024DeviceInformationParserTest {
    private val fixed = byteArrayOf(
        0x34, 0x12, 0x78, 0x56, 0x04, 0x03, 0x02, 0x01,
        0xBC.toByte(), 0x9A.toByte(), 0xF0.toByte(), 0x00,
        1, 65, 1, 66, 1, 67
    )

    @Test fun parsesLittleEndianAndStrings() {
        val result = Cirqa5024DeviceInformationParser.parse(fixed)!!
        assertEquals(0x1234, result.protocolVersion)
        assertEquals(0x5678, result.productNumber)
        assertEquals(0x01020304L, result.unitId)
        assertEquals(0x9ABC, result.softwareVersion)
        assertEquals(0xF0, result.maxPacketSize)
        assertEquals(listOf("A", "B", "C"), result.strings)
        assertEquals(0, result.optionalTrailingBytes)
    }

    @Test fun allTruncatedPrefixesAreRejected() {
        for (length in 0 until fixed.size)
            assertNull("truncated at $length",
                Cirqa5024DeviceInformationParser.parse(fixed.copyOf(length)))
    }

    @Test fun invalidStringLengthRejected() {
        val bad = fixed.clone()
        bad[12] = 100
        assertNull(Cirqa5024DeviceInformationParser.parse(bad))
    }

    @Test fun trailingExtensionIsReportedButNotInterpreted() {
        val result = Cirqa5024DeviceInformationParser.parse(fixed + byteArrayOf(1, 2, 3))!!
        assertEquals(3, result.optionalTrailingBytes)
    }

    @Test fun malformedUtf8IsRejected() {
        val bad = fixed.clone()
        bad[13] = 0xFF.toByte()
        assertNull(Cirqa5024DeviceInformationParser.parse(bad))
    }
}
