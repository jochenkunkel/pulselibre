package de.pulselibre

import org.junit.Assert.*
import org.junit.Test

class CirqaLegacyOfflineFileSessionTest {
    private fun session(index: Int) =
        CirqaLegacyOfflineFileSession(index, { payload, crc ->
            payload.sumOf { it.toInt() and 255 } == crc
        })

    private fun chunk(offset: Int, payload: ByteArray): ByteArray {
        val crc = payload.sumOf { it.toInt() and 255 }
        return byteArrayOf(0, crc.toByte(), (crc ushr 8).toByte(),
            offset.toByte(), (offset ushr 8).toByte(),
            (offset ushr 16).toByte(), (offset ushr 24).toByte()) + payload
    }

    @Test fun offlineDirectoryTransferEndToEnd() {
        val s = session(0)
        assertNull(s.accept5004(chunk(0, byteArrayOf(1))))
        val directoryBytes = ByteArray(32)
        directoryBytes[16] = 0x34
        directoryBytes[17] = 0x12
        directoryBytes[18] = 0x80.toByte()
        directoryBytes[19] = 4
        directoryBytes[23] = 0x80.toByte()
        assertTrue(s.beginWithVerifiedSize(byteArrayOf(0, 0, 0, 0, 0), 32))
        assertEquals(CirqaLegacyPlainFileAssembler.Result.ACCEPTED,
            s.accept5004(chunk(0, directoryBytes.copyOfRange(0, 16))))
        assertEquals(CirqaLegacyPlainFileAssembler.Result.COMPLETE,
            s.accept5004(chunk(16, directoryBytes.copyOfRange(16, 32))))
        assertEquals(CirqaLegacyOfflineFileSession.Phase.COMPLETE, s.phase)
        assertEquals(0x1234, s.completedDirectory()!!.entries.single().fileIndex)
        assertEquals(4, s.completedDirectory()!!.entries.single().subtype)
        assertTrue(s.completedDirectory()!!.entries.single().readable)
        assertFalse(s.beginWithVerifiedSize(byteArrayOf(0, 0, 0, 0, 0), 32))
    }

    @Test fun rejectsFailedReadResponseAndMalformedSize() {
        val a = session(1)
        assertFalse(a.beginWithVerifiedSize(byteArrayOf(3), 8))
        assertEquals(CirqaLegacyOfflineFileSession.Phase.FAILED, a.phase)
        assertNull(a.completedFile())
        val b = session(0)
        assertFalse(b.beginWithVerifiedSize(byteArrayOf(0, 0, 0, 0, 0), 0))
        assertEquals(CirqaLegacyOfflineFileSession.Phase.FAILED, b.phase)
    }

    @Test fun fileTransferDoesNotPretendToBeDirectory() {
        val s = session(12)
        assertTrue(s.beginWithVerifiedSize(byteArrayOf(0, 0, 0, 0, 0), 2))
        assertEquals(CirqaLegacyPlainFileAssembler.Result.COMPLETE,
            s.accept5004(chunk(0, byteArrayOf(1, 2))))
        assertArrayEquals(byteArrayOf(1, 2), s.completedFile())
        assertNull(s.completedDirectory())
    }
}
