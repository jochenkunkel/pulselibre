package de.pulselibre

import org.junit.Assert.*
import org.junit.Test

/** Offline wire tests: never connect to or write to a CIRQA device. */
class GarminFileSyncWireTest {
    @Test fun requestMatchesReferenceEnvelopeAndCrc() {
        val frame = GarminFileListRequest.gfdi()
        assertEquals(13, frame.size)
        assertEquals(0x8A39, u16(frame, 2))
        assertEquals(1, u16(frame, 4))
        assertArrayEquals(
            byteArrayOf(0xDA.toByte(), 0x02, 0x02, 0x4A, 0x00),
            frame.copyOfRange(6, 11)
        )
        assertNotNull(GarminFrameValidator.inspectVerified(frame))
    }

    @Test fun cobsRoundTripPreservesZerosAndPacketBounds() {
        val encoded = GarminFileListRequest.cobs(GarminFileListRequest.gfdi())
        assertEquals(0, encoded.first().toInt())
        assertEquals(0, encoded.last().toInt())
        val probe = GarminFileListProbe()
        probe.observeMlPayload(encoded)
        assertEquals(1, probe.frames)
        assertEquals(0, probe.rejectedFrames)
    }

    @Test fun unsupportedResponseIsParsedWithoutLeakingPayload() {
        val body = byteArrayOf(0x39, 0x8A.toByte(), 0x02)
        val header = byteArrayOf(9, 0, 0x88.toByte(), 0x13)
        val partial = header + body
        val crc = GarminFrameValidator.crc16Arc(partial)
        val frame = partial + byteArrayOf(crc.toByte(), (crc ushr 8).toByte())
        val probe = GarminFileListProbe()
        probe.observeMlPayload(GarminFileListRequest.cobs(frame))
        assertEquals(1, probe.frames)
        assertEquals(1, probe.responseErrors)
        assertEquals(1, probe.responseStatuses["request=35385/status=UNSUPPORTED/seq=-1"])
    }

    private fun u16(bytes: ByteArray, offset: Int): Int =
        (bytes[offset].toInt() and 255) or ((bytes[offset + 1].toInt() and 255) shl 8)
}
