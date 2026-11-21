package de.pulselibre

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class CirqaItemListFingerprintTest {
    private fun item(uid: ByteArray, size: Long = 10L, flags: Long = 0L) =
        CirqaFileAccessItemListParser.Item(
            uid = uid,
            dataType = byteArrayOf(1, 2),
            dataSize = size,
            flags = flags
        )

    @Test fun fingerprintIsIndependentOfListOrder() {
        val a = item(byteArrayOf(1))
        val b = item(byteArrayOf(2))
        assertEquals(
            CirqaItemListFingerprint.create(listOf(a, b)),
            CirqaItemListFingerprint.create(listOf(b, a))
        )
    }

    @Test fun fingerprintChangesWhenIdentityOrMetadataChanges() {
        val baseline = CirqaItemListFingerprint.create(listOf(item(byteArrayOf(1))))
        assertNotEquals(baseline, CirqaItemListFingerprint.create(listOf(item(byteArrayOf(2)))))
        assertNotEquals(baseline, CirqaItemListFingerprint.create(listOf(item(byteArrayOf(1), size = 11))))
        assertNotEquals(baseline, CirqaItemListFingerprint.create(listOf(item(byteArrayOf(1), flags = 1))))
    }

    @Test fun fingerprintIsFixedLengthSha256HexEvenForEmptyList() {
        val digest = CirqaItemListFingerprint.create(emptyList())
        assertEquals(64, digest.length)
        assertEquals(digest.lowercase(), digest)
    }
}
