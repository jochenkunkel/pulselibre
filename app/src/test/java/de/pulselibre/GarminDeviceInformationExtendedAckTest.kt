package de.pulselibre

import org.junit.Assert.*
import org.junit.Test

class GarminDeviceInformationExtendedAckTest {
    @Test fun extendedResponseRetainsHeaderStatusAndValidCrc() {
        // Synthetic extension; no captured Garmin watch identity is embedded.
        val extension = byteArrayOf(0x11, 0x22, 0x33, 0x44)
        val frame = GarminDeviceInformationAckCandidate.extendedFrame(extension)
        assertEquals(13, frame.size)
        assertEquals(13, (frame[0].toInt() and 255) or ((frame[1].toInt() and 255) shl 8))
        assertEquals(5000, (frame[2].toInt() and 255) or ((frame[3].toInt() and 255) shl 8))
        assertArrayEquals(byteArrayOf(0xA0.toByte(), 0x13, 0), frame.copyOfRange(4, 7))
        assertArrayEquals(extension, frame.copyOfRange(7, 11))
        assertNotNull(GarminFrameValidator.inspectVerified(frame))
        assertEquals(9, GarminDeviceInformationAckCandidate.frame().size)
    }

    @Test fun rejectsEmptyAndOversizedExtension() {
        try { GarminDeviceInformationAckCandidate.extendedFrame(byteArrayOf()); fail("empty") }
        catch (_: IllegalArgumentException) { }
        try { GarminDeviceInformationAckCandidate.extendedFrame(ByteArray(513)); fail("oversized") }
        catch (_: IllegalArgumentException) { }
    }
}
