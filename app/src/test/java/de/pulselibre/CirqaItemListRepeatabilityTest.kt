package de.pulselibre

import org.junit.Assert.assertEquals
import org.junit.Test

class CirqaItemListRepeatabilityTest {
    private fun item(uid: ByteArray, dataType: ByteArray, size: Long, flags: Long = 0) =
        CirqaFileAccessItemListParser.Item(uid, dataType, size, flags)

    @Test
    fun identicalListingsMatchWithoutExposingIdentifiers() {
        val first = listOf(item(byteArrayOf(1), byteArrayOf(9), 100))
        val second = listOf(item(byteArrayOf(1), byteArrayOf(9), 100))

        val result = CirqaItemListRepeatability.compare(first, second)

        assertEquals(1, result.unchanged)
        assertEquals(0, result.changedMetadata)
        assertEquals(0, result.added)
        assertEquals(0, result.missing)
    }

    @Test
    fun identifiesChangedMetadataAddedAndMissingItems() {
        val first = listOf(
            item(byteArrayOf(1), byteArrayOf(9), 100),
            item(byteArrayOf(2), byteArrayOf(8), 200)
        )
        val second = listOf(
            item(byteArrayOf(1), byteArrayOf(9), 101),
            item(byteArrayOf(3), byteArrayOf(8), 300)
        )

        val result = CirqaItemListRepeatability.compare(first, second)

        assertEquals(0, result.unchanged)
        assertEquals(1, result.changedMetadata)
        assertEquals(1, result.added)
        assertEquals(1, result.missing)
    }

    @Test
    fun duplicateUidsAreCountedAsAmbiguous() {
        val duplicated = listOf(
            item(byteArrayOf(1), byteArrayOf(9), 100),
            item(byteArrayOf(1), byteArrayOf(9), 100)
        )

        val result = CirqaItemListRepeatability.compare(duplicated, duplicated)

        assertEquals(1, result.duplicateUidsFirst)
        assertEquals(1, result.duplicateUidsSecond)
    }
}
