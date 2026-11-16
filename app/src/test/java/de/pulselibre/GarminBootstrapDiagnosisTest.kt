package de.pulselibre

import org.junit.Assert.*
import org.junit.Test

class GarminBootstrapDiagnosisTest {
    private fun frame(type: Int): ByteArray {
        val header = byteArrayOf(6, 0, type.toByte(), (type ushr 8).toByte())
        val crc = GarminFrameValidator.crc16Arc(header)
        return header + byteArrayOf(crc.toByte(), (crc ushr 8).toByte())
    }

    @Test fun repeatedDeviceInformationDoesNotImplyFileSyncSupport() {
        val diagnostic = GarminBootstrapDiagnosis()
        repeat(4) { diagnostic.observe(frame(5024)) }
        assertEquals(4, diagnostic.validFrames)
        assertEquals(3, diagnostic.deviceInfoRepeats)
        assertEquals(0, diagnostic.statusFrames)
        assertEquals(0, diagnostic.otherFrames)
        assertTrue(diagnostic.summary().contains("aktive Bootstrap-Befehle=0"))
    }

    @Test fun ignoresBadCrcAndSeparatesOtherTypes() {
        val diagnostic = GarminBootstrapDiagnosis()
        val invalid = frame(5024)
        invalid[4] = (invalid[4].toInt() xor 1).toByte()
        diagnostic.observe(invalid)
        diagnostic.observe(frame(5000))
        diagnostic.observe(frame(5044))
        assertEquals(2, diagnostic.validFrames)
        assertEquals(0, diagnostic.deviceInfoRepeats)
        assertEquals(1, diagnostic.statusFrames)
        assertEquals(1, diagnostic.otherFrames)
    }
}
