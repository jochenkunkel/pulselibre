package de.pulselibre

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class FitFieldSampleReaderTest {
    private fun u32(value: Int) = byteArrayOf(value.toByte(), (value ushr 8).toByte(), (value ushr 16).toByte(), (value ushr 24).toByte())

    private fun fit(data: ByteArray): ByteArray {
        val file = ByteArray(14 + data.size + 2)
        file[0] = 14; file[1] = 0x20; file[2] = 0x64
        file[4] = data.size.toByte(); file[5] = (data.size ushr 8).toByte()
        ".FIT".toByteArray().copyInto(file, 8)
        val headerCrc = FitFileHeader.crc16(file, 0, 12)
        file[12] = headerCrc.toByte(); file[13] = (headerCrc ushr 8).toByte()
        data.copyInto(file, 14)
        val crc = FitFileHeader.crc16(file, 0, file.size - 2)
        file[file.size - 2] = crc.toByte(); file[file.lastIndex] = (crc ushr 8).toByte()
        return file
    }

    // Definition: global 20, fields 253 (u32 timestamp), 85 (u16 unknown); then N data messages.
    private fun standardFile(values: List<Int>): ByteArray {
        var data = byteArrayOf(0x40, 0, 0, 20, 0, 2, 253.toByte(), 4, 0x86.toByte(), 85, 2, 0x84.toByte())
        values.forEachIndexed { i, v -> data += byteArrayOf(0) + u32(100_000 + i) + byteArrayOf(v.toByte(), (v ushr 8).toByte()) }
        return fit(data)
    }

    @Test fun readsRawBytesOfAStandardFieldAndCountsAll() {
        val preview = FitFieldSampleReader.read(standardFile(listOf(0x007B, 0x0102, 0xAA55, 7, 8)), 20, 85, null)!!
        assertEquals(5, preview.occurrences)
        assertEquals(FitFieldSampleReader.MAX_SAMPLES, preview.samples.size)
        assertEquals("7B 00", FitFieldSampleReader.hex(preview.samples[0]))
        assertEquals("02 01", FitFieldSampleReader.hex(preview.samples[1]))
        assertEquals("55 AA", FitFieldSampleReader.hex(preview.samples[2]))
        assertFalse(preview.truncatedBytes)
    }

    @Test fun readsADeveloperFieldOnlyWithItsIndex() {
        // Definition with developer flag: global 20, field 253, developer field 7 (3 bytes, index 1).
        val data = byteArrayOf(0x60, 0, 0, 20, 0, 1, 253.toByte(), 4, 0x86.toByte(), 1, 7, 3, 1) +
            byteArrayOf(0) + u32(100_000) + byteArrayOf(0x0A, 0x0B, 0x0C)
        val file = fit(data)
        assertEquals("0A 0B 0C", FitFieldSampleReader.hex(FitFieldSampleReader.read(file, 20, 7, 1)!!.samples.single()))
        assertEquals(0, FitFieldSampleReader.read(file, 20, 7, 2)!!.occurrences)   // other developer index
        assertEquals(0, FitFieldSampleReader.read(file, 20, 7, null)!!.occurrences) // not a standard field
    }

    @Test fun compressedTimestampHeaderSkipsTheTimestampBytes() {
        // Normal data message first (sets the timestamp), then a compressed-header message (local type 0, offset 5).
        val data = byteArrayOf(0x40, 0, 0, 20, 0, 2, 253.toByte(), 4, 0x86.toByte(), 85, 2, 0x84.toByte(),
            0) + u32(100_000) + byteArrayOf(1, 0) +
            byteArrayOf((0x80 or 5).toByte(), 0x34, 0x12)
        val preview = FitFieldSampleReader.read(fit(data), 20, 85, null)!!
        assertEquals(2, preview.occurrences)
        assertEquals("34 12", FitFieldSampleReader.hex(preview.samples[1]))
    }

    @Test fun longValuesAreCutAndFlagged() {
        val big = ByteArray(40) { it.toByte() }
        val data = byteArrayOf(0x40, 0, 0, 20, 0, 2, 253.toByte(), 4, 0x86.toByte(), 85, 40, 0x0D) +
            byteArrayOf(0) + u32(100_000) + big
        val preview = FitFieldSampleReader.read(fit(data), 20, 85, null)!!
        assertEquals(FitFieldSampleReader.MAX_BYTES_PER_SAMPLE, preview.samples.single().size)
        assertTrue(preview.truncatedBytes)
    }

    @Test fun missingFieldGivesZeroOccurrencesNotAnError() {
        assertEquals(0, FitFieldSampleReader.read(standardFile(listOf(1)), 20, 99, null)!!.occurrences)
        assertEquals(0, FitFieldSampleReader.read(standardFile(listOf(1)), 21, 85, null)!!.occurrences)
    }

    @Test fun damagedOrTruncatedOriginalFailsClosed() {
        val good = standardFile(listOf(1, 2))
        val badCrc = good.copyOf().also { it[it.size - 1] = (it[it.size - 1] + 1).toByte() }
        assertNull(FitFieldSampleReader.read(badCrc, 20, 85, null))
        assertNull(FitFieldSampleReader.read(good.copyOf(good.size - 3), 20, 85, null))
        assertNull(FitFieldSampleReader.read(ByteArray(0), 20, 85, null))
        assertNotNull(FitFieldSampleReader.read(good, 20, 85, null))
    }

    @Test fun describeNamesFilesByOrdinalAndNeverByHash() {
        val preview = FitFieldSampleReader.read(standardFile(listOf(1, 2, 3, 4, 5)), 20, 85, null)
        val text = FitFieldSampleReader.describe(2, preview)
        assertTrue(text, text.startsWith("Datei 2: 5 Vorkommen, erste 3 Werte"))
        assertTrue(text, text.contains("… und 2 weitere"))
        assertTrue(text, text.contains("  1: 01 00"))
        assertEquals("Datei 1: Original nicht lesbar oder nicht gültig.", FitFieldSampleReader.describe(1, null))
        assertEquals("Datei 1: Feld in dieser Datei nicht vorhanden.",
            FitFieldSampleReader.describe(1, FitFieldSampleReader.read(standardFile(listOf(1)), 20, 99, null)))
    }
}
