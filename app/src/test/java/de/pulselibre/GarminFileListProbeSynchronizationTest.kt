package de.pulselibre

import org.junit.Assert.*
import org.junit.Test

class GarminFileListProbeSynchronizationTest {
    private fun frame(body: ByteArray): ByteArray {
        val size = body.size + 6
        val frame = ByteArray(size)
        frame[0] = size.toByte()
        frame[1] = (size ushr 8).toByte()
        frame[2] = 0x25 // compact GFDI 5037 = 5000 + 0x25
        frame[3] = 0x80.toByte()
        body.copyInto(frame, 4)
        val crc = GarminFrameValidator.crc16Arc(frame, 0, size - 2)
        frame[size - 2] = crc.toByte()
        frame[size - 1] = (crc ushr 8).toByte()
        return frame
    }

    @Test fun passiveParserCountsValid5037BodyAfterCrc() {
        val probe = GarminFileListProbe()
        probe.observeMlPayload(GarminFileListRequest.cobs(
            frame(byteArrayOf(2, 4, 8, 0, 0, 4))
        ))
        assertEquals(1, probe.validSynchronizationShapes)
        assertEquals(0, probe.malformedSynchronizationShapes)
        assertEquals(1, probe.synchronizationMaskBitCounts[2])
        assertEquals(1, probe.frameTypes["gfdi-5037"])
    }

    @Test fun malformedBodyIsNotTreatedAsSync() {
        val probe = GarminFileListProbe()
        probe.observeMlPayload(GarminFileListRequest.cobs(
            frame(byteArrayOf(2, 4, 8))
        ))
        assertEquals(0, probe.validSynchronizationShapes)
        assertEquals(1, probe.malformedSynchronizationShapes)
    }

    @Test fun corruptedCrcNeverReaches5037Inspection() {
        val probe = GarminFileListProbe()
        val corrupted = frame(byteArrayOf(2, 4, 8, 0, 0, 4))
        corrupted[5] = (corrupted[5].toInt() xor 1).toByte()
        probe.observeMlPayload(GarminFileListRequest.cobs(corrupted))
        assertEquals(0, probe.validSynchronizationShapes)
        assertEquals(0, probe.malformedSynchronizationShapes)
        assertEquals(1, probe.rejectedFrames)
    }
}
