package de.pulselibre

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CirqaLegacy5002ActiveCodecTest {
    @Test fun buildsIndexZeroUncompressedConnectRequest() {
        val frame = CirqaLegacy5002RequestCandidate.gfdiFrame(0)
        assertEquals(5002, GarminGfdiHeader.inspect(frame)?.type)
        assertTrue(GarminFrameValidator.inspectVerified(frame) != null)
        assertEquals(20, frame.size)
        assertEquals(
            listOf(0, 0, 0, 0, 0, 0, 1, 0, 0, 0, 0, 0, 0, 0),
            CirqaLegacy5002Codec.request(0, compressed = false).map { it.toInt() and 255 }
        )
    }

    @Test fun parsesSingleSuccessfulDirectoryReadResponse() {
        val responseBody = byteArrayOf(0, 16, 0, 0, 0)
        val prefix = byteArrayOf(
            14, 0, 0x88.toByte(), 0x13, 0x8A.toByte(), 0x13, 0
        ) + responseBody
        val crc = GarminFrameValidator.crc16Arc(prefix)
        val response = prefix + byteArrayOf(crc.toByte(), (crc ushr 8).toByte())
        val observer = CirqaLegacy5002ResponseObserver()
        assertEquals(
            CirqaLegacy5002ResponseObserver.Result.SUCCESS,
            observer.observeMlPayload(GarminFileListRequest.cobs(response))
        )
        assertEquals(16L, observer.response?.unsignedValue)
    }

    @Test fun onlyAcknowledgementCandidateIsForSuccessful5004Data() {
        val ack = CirqaLegacy5004AckCandidate.gfdiFrame(1024)
        assertTrue(GarminFrameValidator.inspectVerified(ack) != null)
        val header = GarminGfdiHeader.inspect(ack)!!
        assertEquals(5000, header.type)
        assertEquals(5004, header.responseTo)
        assertEquals(0, header.responseStatus)
        assertEquals(14, ack.size)
    }

    @Test fun limitsSelectionToReadableSupportedFitEntries() {
        fun entry(index: Int, type: Int, subtype: Int, flags: Int, size: Long) =
            CirqaLegacyDirectoryParser.Entry(index, type, listOf(subtype, 0, 0), 0, flags, size, 0)
        val selected = CirqaLegacyDownloadLimits.selectReadableSupportedFits(
            listOf(
                entry(1, 128, 4, 0x80, 100),
                entry(2, 128, 32, 0, 100),
                entry(3, 255, 245, 0x80, 100),
                entry(4, 128, 32, 0x80, 100),
                entry(5, 128, 99, 0x80, 100)
            ),
            setOf(128 to 4, 128 to 32)
        )
        assertEquals(listOf(1, 4), selected.map { it.fileIndex })
        assertFalse(selected.any { it.type != 128 })
    }

    @Test fun cachedTypesRequirePriorStatusZeroEvidence() {
        val valid = "Status=0; erkannte Dateitypen=128/4:FIT_TYPE_4, 128/32:FIT_TYPE_32"
        assertEquals(setOf(128 to 4, 128 to 32), CirqaLegacySupportedTypesCache.parse(valid))
        assertTrue(CirqaLegacySupportedTypesCache.parse("Status=2; erkannte Dateitypen=128/4:X").isEmpty())
        assertTrue(CirqaLegacySupportedTypesCache.parse(null).isEmpty())
        assertTrue(CirqaLegacyHistoryDownloadGate.canAttempt(false))
        assertFalse(CirqaLegacyHistoryDownloadGate.canAttempt(true))
    }

    @Test fun observesFragmentedCrcValid5004Frame() {
        val body = byteArrayOf(0, 0xC0.toByte(), 0x30, 0, 0, 0, 0, 'A'.code.toByte())
        val prefix = byteArrayOf(14, 0, 0x8C.toByte(), 0x13) + body
        val crc = GarminFrameValidator.crc16Arc(prefix)
        val frame = prefix + byteArrayOf(crc.toByte(), (crc ushr 8).toByte())
        val wire = GarminFileListRequest.cobs(frame)
        val split = wire.size / 2
        val observer = CirqaLegacy5004ResponseObserver()
        assertEquals(CirqaLegacy5004ResponseObserver.Result.PENDING,
            observer.observeMlPayload(wire.copyOfRange(0, split)))
        assertEquals(CirqaLegacy5004ResponseObserver.Result.DATA,
            observer.observeMlPayload(wire.copyOfRange(split, wire.size)))
        val chunk = CirqaLegacyTransferDecoder.plainChunk(observer.body!!)!!
        assertEquals("A", chunk.payload.toString(Charsets.UTF_8))
        assertEquals(chunk.crc16, CirqaLegacyCrc16.compute(chunk.payload))
    }
}
