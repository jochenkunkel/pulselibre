package de.pulselibre

import org.junit.Assert.*
import org.junit.Test

class CirqaLegacyPlainFileAssemblerTest {
    private fun chunk(offset: Int, crc: Int, vararg payload: Int): ByteArray =
        byteArrayOf(0, crc.toByte(), (crc ushr 8).toByte(),
            offset.toByte(), (offset ushr 8).toByte(),
            (offset ushr 16).toByte(), (offset ushr 24).toByte()) +
            payload.map { it.toByte() }.toByteArray()

    // Fake verifier exercises the assembler contract; it is NOT a Garmin CRC implementation.
    private fun assembler(size: Int) =
        CirqaLegacyPlainFileAssembler(size, { payload, crc ->
            (payload.sumOf { it.toInt() and 255 } and 65535) == crc
        })

    @Test fun reassemblesAndIgnoresExactDuplicate() {
        val a = assembler(4)
        val first = chunk(0, 3, 1, 2)
        assertEquals(CirqaLegacyPlainFileAssembler.Result.ACCEPTED, a.accept(first))
        assertEquals(CirqaLegacyPlainFileAssembler.Result.DUPLICATE, a.accept(first))
        assertEquals(2, a.receivedBytes)
        assertEquals(CirqaLegacyPlainFileAssembler.Result.COMPLETE,
            a.accept(chunk(2, 7, 3, 4)))
        assertArrayEquals(byteArrayOf(1, 2, 3, 4), a.completedBytes())
        assertEquals(CirqaLegacyPlainFileAssembler.Result.COMPLETE, a.accept(first))
    }

    @Test fun rejectsWrongCrcAndOffsetAndAbortsAfterThreeErrors() {
        val a = assembler(3)
        assertEquals(CirqaLegacyPlainFileAssembler.Result.CRC_ERROR,
            a.accept(chunk(0, 9, 1)))
        assertEquals(CirqaLegacyPlainFileAssembler.Result.OFFSET_ERROR,
            a.accept(chunk(2, 1, 1)))
        assertEquals(CirqaLegacyPlainFileAssembler.Result.ABORTED,
            a.accept(byteArrayOf(0)))
        assertTrue(a.isAborted)
        assertNull(a.completedBytes())
    }

    @Test fun rejectsOversizedAndConflictingDuplicate() {
        val a = assembler(2)
        assertEquals(CirqaLegacyPlainFileAssembler.Result.MALFORMED,
            a.accept(chunk(0, 6, 1, 2, 3)))
        assertEquals(CirqaLegacyPlainFileAssembler.Result.ACCEPTED,
            a.accept(chunk(0, 1, 1)))
        assertEquals(CirqaLegacyPlainFileAssembler.Result.OFFSET_ERROR,
            a.accept(chunk(0, 2, 2)))
        assertNull(a.completedBytes())
    }

    @Test fun rejectsNonzeroTransferStatusEvenWithValidCrc() {
        val a = assembler(1)
        val invalidStatus = chunk(0, 7, 7)
        invalidStatus[0] = 1
        assertEquals(CirqaLegacyPlainFileAssembler.Result.MALFORMED,
            a.accept(invalidStatus))
        assertEquals(0, a.receivedBytes)
        assertNull(a.completedBytes())
        assertEquals(CirqaLegacyPlainFileAssembler.Result.COMPLETE,
            a.accept(chunk(0, 7, 7)))
    }

    @Test fun enforcesMemoryLimit() {
        try {
            CirqaLegacyPlainFileAssembler(11, { _, _ -> true }, 10)
            fail("must reject file larger than limit")
        } catch (_: IllegalArgumentException) { }
    }
}
