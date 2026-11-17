package de.pulselibre

import org.junit.Assert.*
import org.junit.Test

class Cirqa5037OfflineSynchronizationTest {
    @Test fun acceptsFourByteLittleEndianMask() {
        val result = Cirqa5037OfflineSynchronization.inspect(
            byteArrayOf(2, 4, 0x08, 0x00, 0x00, 0x04)
        )!!
        assertEquals(2, result.synchronizationType)
        assertEquals(4, result.maskByteCount)
        assertEquals(listOf(3, 26), result.setFileTypeBits)
    }

    @Test fun acceptsEightByteHighBitWithoutSignedByteError() {
        val result = Cirqa5037OfflineSynchronization.inspect(
            byteArrayOf(255.toByte(), 8, 0, 0, 0, 0, 0, 0, 0, 0x80.toByte())
        )!!
        assertEquals(255, result.synchronizationType)
        assertEquals(8, result.maskByteCount)
        assertEquals(listOf(63), result.setFileTypeBits)
    }

    @Test fun rejectsTruncatedExtraAndUnsupportedBodies() {
        val invalid = listOf(
            byteArrayOf(),
            byteArrayOf(1),
            byteArrayOf(1, 4, 0),
            byteArrayOf(1, 4, 0, 0, 0, 0, 0),
            byteArrayOf(1, 5, 0, 0, 0, 0, 0),
            byteArrayOf(1, 8, 0, 0, 0, 0)
        )
        invalid.forEach { assertNull(Cirqa5037OfflineSynchronization.inspect(it)) }
    }

    @Test fun acceptsEmptyMaskWithoutClaimingFileAccess() {
        val result = Cirqa5037OfflineSynchronization.inspect(
            byteArrayOf(0, 4, 0, 0, 0, 0)
        )!!
        assertTrue(result.setFileTypeBits.isEmpty())
        assertEquals(0uL, result.fileTypeMask)
    }
}
