package de.pulselibre

import org.junit.Assert.*
import org.junit.Test

class Cirqa5050OfflineCapabilitiesTest {
    private fun frame(body: ByteArray): ByteArray {
        val size = body.size + 6
        val prefix = byteArrayOf(size.toByte(), (size ushr 8).toByte(),
            5050.toByte(), (5050 ushr 8).toByte()) + body
        val crc = GarminFrameValidator.crc16Arc(prefix)
        return prefix + byteArrayOf(crc.toByte(), (crc ushr 8).toByte())
    }

    @Test fun recognizesLengthPrefixedCapabilitiesWithoutDisclosingBytes() {
        val body = byteArrayOf(18) + ByteArray(18) { if (it == 0) 3 else 0 }
        val shape = Cirqa5050OfflineCapabilities.inspect(frame(body))!!
        assertEquals(19, shape.bodyLength)
        assertEquals(18, shape.declaredLength)
        assertEquals(18, shape.actualLength)
        assertTrue(shape.lengthMatches)
        assertEquals(2, shape.setBits)
    }

    @Test fun rejectsLengthMismatchAndBadCrc() {
        val mismatch = Cirqa5050OfflineCapabilities.inspect(frame(byteArrayOf(3, 1, 2)))!!
        assertFalse(mismatch.lengthMatches)
        val corrupt = frame(byteArrayOf(1, 1))
        corrupt[corrupt.lastIndex] = (corrupt.last().toInt() xor 1).toByte()
        assertNull(Cirqa5050OfflineCapabilities.inspect(corrupt))
        assertNull(Cirqa5050OfflineCapabilities.inspect(GarminDeviceInformationAckCandidate.frame()))
    }
}
