package de.pulselibre

import org.junit.Assert.*
import org.junit.Test

class CirqaLegacyFitFileSelectorTest {
    private fun entry(index: Int, type: Int, subtype: Int, attrs: Int): ByteArray =
        byteArrayOf(index.toByte(), (index ushr 8).toByte(), type.toByte(),
            subtype.toByte(), 0, 0, 0, attrs.toByte(), 0, 0, 0, 0, 0, 0, 0, 0)

    @Test fun selectsOnlyReadableSupportedUploadableFitEntries() {
        val directory = CirqaLegacyDirectoryParser.parse(
            ByteArray(16) +
                entry(0, 128, 4, 128) +
                entry(10, 128, 4, 128) +
                entry(11, 128, 36, 128) +
                entry(12, 128, 4, 0) +
                entry(13, 127, 4, 128) +
                entry(10, 128, 4, 128)
        )!!
        val types = listOf(
            CirqaLegacyFileTypesParser.FileType(128, 4, "FIT_TYPE_4"),
            CirqaLegacyFileTypesParser.FileType(128, 36, "FIT_TYPE_36"))
        assertEquals(listOf(10), CirqaLegacyFitFileSelector.select(
            directory, types, setOf(4)).map { it.fileIndex })
        assertTrue(CirqaLegacyFitFileSelector.select(
            directory, types, emptySet()).isEmpty())
        assertTrue(CirqaLegacyFitFileSelector.select(
            directory, emptyList(), setOf(4)).isEmpty())
    }

    @Test fun noImplicitSubtypeFallback() {
        val directory = CirqaLegacyDirectoryParser.parse(
            ByteArray(16) + entry(42, 128, 4, 128))!!
        assertTrue(CirqaLegacyFitFileSelector.select(
            directory,
            listOf(CirqaLegacyFileTypesParser.FileType(128, 36, "FIT_TYPE_36")),
            setOf(4)).isEmpty())
    }
}
