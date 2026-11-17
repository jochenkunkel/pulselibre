package de.pulselibre

import org.junit.Assert.*
import org.junit.Test

class GarminDeviceInformationReferenceTest {
    @Test fun publishedFenix8FrameHasExpectedStructure() {
        // Reference test fixture only; this frame is NEVER connected to BLE transmission.
        val hex = "34008813a013009700b811a13309d85b08a00f0e66656e69782038202d2035316d6d" +
            "0566656e69780838202d2035316d6d030fd7"
        val frame = hex.chunked(2).map { it.toInt(16).toByte() }.toByteArray()
        val structure = GarminDeviceInformationReference.inspect(frame)
        assertNotNull(structure)
        assertEquals(52, structure!!.frameBytes)
        assertEquals(43, structure.extensionBytes)
        assertEquals(12, structure.leadingBinaryBytes)
        assertEquals(14, structure.firstTextBytes)
        assertEquals(5, structure.secondTextBytes)
        assertEquals(8, structure.thirdTextBytes)
        assertEquals(1, structure.trailingBinaryBytes)
    }

    @Test fun malformedTextLengthIsRejectedEvenWithCorrectCrc() {
        val extension = ByteArray(43)
        extension[12] = 99.toByte() // would exceed extension bounds
        val frame = GarminDeviceInformationAckCandidate.extendedFrame(extension)
        assertNull(GarminDeviceInformationReference.inspect(frame))
    }

    @Test fun alteredCrcIsRejected() {
        val frame = GarminDeviceInformationAckCandidate.extendedFrame(ByteArray(43))
        frame[frame.lastIndex] = (frame.last().toInt() xor 1).toByte()
        assertNull(GarminDeviceInformationReference.inspect(frame))
    }

    @Test fun minimalResponseHasNoReferenceLayout() {
        assertNull(GarminDeviceInformationReference.inspect(GarminDeviceInformationAckCandidate.frame()))
    }
}
