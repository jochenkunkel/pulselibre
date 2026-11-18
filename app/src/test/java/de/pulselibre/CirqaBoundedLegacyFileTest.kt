package de.pulselibre

import java.nio.file.Files
import org.junit.Assert.*
import org.junit.Test

class CirqaBoundedLegacyFileTest {
    private val fit = byteArrayOf(12, 32, 100, 0, 0, 0, 0, 0, 46, 70, 73, 84, 39, 41)

    private fun gfdi(type: Int, body: ByteArray): ByteArray {
        val size = body.size + 6
        val prefix = byteArrayOf(size.toByte(), (size ushr 8).toByte(), type.toByte(),
            (type ushr 8).toByte()) + body
        val crc = CirqaLegacyCrc16.compute(prefix)
        return prefix + byteArrayOf(crc.toByte(), (crc ushr 8).toByte())
    }

    private fun response(size: Long, status: Int = 0): ByteArray = gfdi(5000,
        byteArrayOf(0x8a.toByte(), 0x13, 0, status.toByte(),
            size.toByte(), (size ushr 8).toByte(), (size ushr 16).toByte(), (size ushr 24).toByte()))

    private fun chunk(data: ByteArray, offset: Long, crc: Int): ByteArray = gfdi(5004,
        byteArrayOf(0, crc.toByte(), (crc ushr 8).toByte(), offset.toByte(),
            (offset ushr 8).toByte(), (offset ushr 16).toByte(), (offset ushr 24).toByte()) + data)

    private fun directory(index: Int = 7): ByteArray = ByteArray(32).apply {
        this[16] = index.toByte()
        this[17] = (index ushr 8).toByte()
        this[18] = 128.toByte()
        this[19] = 4
        this[23] = 128.toByte()
        this[24] = fit.size.toByte()
    }

    private fun receiving(): CirqaBoundedLegacyFile = CirqaBoundedLegacyFile(7, fit.size.toLong()).apply {
        assertNotNull(claim(0))
        assertTrue(writeCompleted(token, true, 1))
        assertTrue(response5002(token, response(fit.size.toLong()), 2))
    }

    private fun transfer(s: CirqaBoundedLegacyFile, data: ByteArray, base: Long = 10) {
        assertNotNull(s.claim(base))
        assertTrue(s.response5002(s.token, response(data.size.toLong()), base + 1))
        assertTrue(s.writeCompleted(s.token, true, base + 2))
        val first = data.copyOfRange(0, data.size / 2)
        val second = data.copyOfRange(first.size, data.size)
        val crc1 = CirqaLegacyCrc16.compute(first)
        val crc2 = CirqaLegacyCrc16.compute(second, crc1)
        assertEquals(first.size, s.chunk5004(s.token, chunk(first, 0, crc1), base + 3))
        assertNull(s.completedBytes())
        assertEquals(data.size, s.chunk5004(s.token, chunk(second, first.size.toLong(), crc2), base + 4))
    }

    @Test fun syntheticEndToEndDirectorySelectionFitIntegrityStorageAndDedupe() {
        // Cached 5031 types stand in for a prior verified result, not a new 5031 request.
        // Only this synthetic fixture supplies positive inner-result mappings.
        val transition = CirqaConnectDirectoryTransition(mapOf(5030 to setOf(0), 5007 to setOf(0)))
        assertTrue(transition.begin(true, true, 0))
        assertTrue(transition.bootstrapCompleted(true, true, true, true, true, false, false, 1))
        assertTrue(transition.claimSystemEvent(2))
        assertTrue(transition.writeCompleted(5030, true, 3))
        assertTrue(transition.response(gfdi(5000, byteArrayOf(0xa6.toByte(), 0x13, 0, 0)), 4))
        assertTrue(transition.claimFilter(5))
        assertTrue(transition.writeCompleted(5007, true, 6))
        assertTrue(transition.response(gfdi(5000, byteArrayOf(0x8f.toByte(), 0x13, 0, 0)), 7))
        assertTrue(transition.claimDirectory(8))
        val dir = CirqaBoundedLegacyFile(0, sessionDeadlineMillis = CirqaBoundedLegacyFile.MAX_MILLIS)
        transfer(dir, directory())
        val entries = CirqaLegacyDirectoryParser.parse(dir.completedBytes()!!)!!.entries
        val selected = CirqaLegacyDownloadLimits.selectReadableSupportedFits(entries, setOf(128 to 4))
        assertEquals(listOf(7), selected.map { it.fileIndex })
        val file = CirqaBoundedLegacyFile(selected.single().fileIndex, selected.single().size,
            sessionDeadlineMillis = CirqaBoundedLegacyFile.MAX_MILLIS)
        transfer(file, fit, base = 20)
        val root = Files.createTempDirectory("cirqa-synthetic-e2e").toFile()
        try {
            val store = CirqaVerifiedFitStore(root)
            val saved = store.saveVerified(file.completedBytes()!!)
            assertArrayEquals(fit, saved.file.readBytes())
            assertNotNull(FitFileHeader.inspectVerified(saved.file.readBytes()))
            assertEquals(CirqaVerifiedFitStore.Outcome.ALREADY_PRESENT,
                store.saveVerified(file.completedBytes()!!).outcome)
            assertEquals(1, root.listFiles()!!.size)
        } finally {
            root.listFiles()?.forEach { it.delete() }
            root.delete()
        }
    }

    @Test fun allFitChunkSplitsUseCumulativeCrcNotIndependentPerChunkCrc() {
        for (split in 1 until fit.size) {
            val s = receiving()
            val first = fit.copyOfRange(0, split)
            val second = fit.copyOfRange(split, fit.size)
            val crc1 = CirqaLegacyCrc16.compute(first)
            assertEquals(split, s.chunk5004(s.token, chunk(first, 0, crc1), 3))
            assertEquals(fit.size, s.chunk5004(s.token, chunk(second, split.toLong(),
                CirqaLegacyCrc16.compute(second, crc1)), 4))
            assertArrayEquals(fit, s.completedBytes())
        }
    }

    @Test fun rejectsDuplicateOffsetCrcOuterCrcAndInvalidFitWithoutExposingPartialFile() {
        val badFit = fit.copyOf().also { it[13] = 0 }
        val cases = listOf(
            chunk(fit, 1, CirqaLegacyCrc16.compute(fit)),
            chunk(fit, 0, 1),
            chunk(fit, 0, CirqaLegacyCrc16.compute(fit)).also { it[4] = 1 },
            chunk(badFit, 0, CirqaLegacyCrc16.compute(badFit)),
            chunk(fit + byteArrayOf(0), 0, CirqaLegacyCrc16.compute(fit + byteArrayOf(0)))
        )
        for (frame in cases) {
            val s = receiving()
            assertNull(s.chunk5004(s.token, frame, 3))
            assertEquals(CirqaBoundedLegacyFile.Phase.FAILED, s.phase)
            assertNull(s.completedBytes())
        }
        val duplicate = receiving()
        val first = fit.copyOfRange(0, 3)
        val frame = chunk(first, 0, CirqaLegacyCrc16.compute(first))
        assertEquals(3, duplicate.chunk5004(duplicate.token, frame, 3))
        assertNull(duplicate.chunk5004(duplicate.token, frame, 4))
        assertNull(duplicate.completedBytes())
    }

    @Test fun readSizeErrorsNotReadyAndLateResponsesAreTerminal() {
        for (response in listOf(
            response(0), response(13), response(0xffffffffL), response(14, status = 3)
        )) {
            val s = CirqaBoundedLegacyFile(7, 14)
            s.claim(0)
            assertFalse(s.response5002(s.token, response, 1))
            assertNull(s.claim(2))
        }
        val duplicate = receiving()
        assertFalse(duplicate.response5002(duplicate.token, response(14), 3))
        val timeout = receiving()
        assertFalse(timeout.tick(timeout.token, CirqaBoundedLegacyFile.MAX_MILLIS))
        assertNull(timeout.completedBytes())
        val disconnected = receiving()
        assertFalse(disconnected.disconnect(disconnected.token))
        assertNull(disconnected.completedBytes())
    }

    @Test fun staleSessionsAndDataBeforeWriteCannotReleaseAnything() {
        val s = CirqaBoundedLegacyFile(7, 14)
        s.claim(0)
        assertTrue(s.response5002(s.token, response(14), 1))
        val oldToken = Any()
        assertFalse(s.writeCompleted(oldToken, true, 2))
        assertFalse(s.disconnect(oldToken))
        assertEquals(CirqaBoundedLegacyFile.Phase.AWAITING_RESPONSE, s.phase)
        assertNull(s.chunk5004(s.token, chunk(fit, 0, CirqaLegacyCrc16.compute(fit)), 3))
        assertNull(s.completedBytes())
    }

    @Test fun directoryRejectsDuplicatesTruncationOversizeAndReservedFitIndex() {
        assertNull(CirqaLegacyDirectoryParser.parseForDownload(directory() + directory().copyOfRange(16, 32)))
        assertNull(CirqaLegacyDirectoryParser.parse(directory().copyOf(31)))
        assertNull(CirqaLegacyDirectoryParser.parse(ByteArray(
            CirqaLegacyDownloadLimits.MAX_DIRECTORY_BYTES + 16)))
        val reserved = CirqaLegacyDirectoryParser.parse(directory(0))!!.entries
        assertTrue(CirqaLegacyDownloadLimits.selectReadableSupportedFits(reserved, setOf(128 to 4)).isEmpty())
    }

    @Test fun everyFileUsesParentSessionDeadlineRatherThanRefreshingTheBudget() {
        val late = CirqaBoundedLegacyFile(7, 14, sessionDeadlineMillis = 100)
        assertNotNull(late.claim(90))
        assertTrue(late.writeCompleted(late.token, true, 91))
        assertFalse(late.response5002(late.token, response(14), 100))
        assertNull(late.completedBytes())
        assertNull(CirqaBoundedLegacyFile(7, 14, sessionDeadlineMillis = 100).claim(100))
    }
}
