package de.pulselibre

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.ByteArrayOutputStream
import java.util.zip.DeflaterOutputStream

/**
 * Offline tests for the #149 probe hardening: fail-closed sync_response
 * recognition, bounded size-agnostic zlib decoding and MLR data-handle
 * normalization. All bytes are synthetic; no device capture or BLE is used.
 */
class CirqaS149ProbeHardeningTest {

    /** Device-confirmed shape: Smart{13: CoreService{2: sync_response{...}}}. */
    private fun syncResponse(): ByteArray = byteArrayOf(
        0x6a, 0x04,          // field 13 (core_service), length 4
        0x12, 0x02,          // field 2 (sync_response), length 2
        0x18, 0x00           // opaque status payload; not interpreted offline
    )

    @Test fun acceptsConfirmedSyncResponseShape() {
        assertTrue(CirqaSyncResponseParser.isSyncResponse(syncResponse()))
        assertEquals("13:2[2:2]", CirqaProtobufShapeInspector.nestedShape(syncResponse()))
    }

    @Test fun rejectsOtherProtobufMessages() {
        // file_access_service item_list_response (Smart field 43)
        val itemList = byteArrayOf(0xe2.toByte(), 0x02, 0x04, 0x12, 0x02, 0x08, 0x01)
        assertFalse(CirqaSyncResponseParser.isSyncResponse(itemList))
        assertFalse(CirqaSyncResponseParser.isSyncResponse(byteArrayOf()))
        assertFalse(CirqaSyncResponseParser.isSyncResponse(byteArrayOf(0x6a)))
        // field 13 present but nested field is not sync_response
        assertFalse(CirqaSyncResponseParser.isSyncResponse(
            byteArrayOf(0x6a, 0x02, 0x0a, 0x00)
        ))
        // truncated length-delimited field
        assertFalse(CirqaSyncResponseParser.isSyncResponse(
            byteArrayOf(0x6a, 0x08, 0x12, 0x02, 0x18, 0x00)
        ))
    }

    private fun syntheticFit(): ByteArray {
        val data = byteArrayOf(
            0x40, 0, 0, 20, 0, 2,
            253.toByte(), 4, 0x86.toByte(),
            3, 1, 2,
            0
        ) + byteArrayOf(0x10, 0x27, 0, 0, 72)
        val file = ByteArray(14 + data.size + 2)
        file[0] = 14
        file[1] = 0x20
        file[2] = 0x64
        file[4] = data.size.toByte()
        file[5] = (data.size ushr 8).toByte()
        ".FIT".toByteArray().copyInto(file, 8)
        val headerCrc = FitFileHeader.crc16(file, 0, 12)
        file[12] = headerCrc.toByte()
        file[13] = (headerCrc ushr 8).toByte()
        data.copyInto(file, 14)
        val crc = FitFileHeader.crc16(file, 0, file.size - 2)
        file[file.size - 2] = crc.toByte()
        file[file.size - 1] = (crc ushr 8).toByte()
        return file
    }

    private fun zlib(bytes: ByteArray): ByteArray = ByteArrayOutputStream().also { output ->
        DeflaterOutputStream(output).use { it.write(bytes) }
    }.toByteArray()

    @Test fun decodesCompleteZlibStreamWithoutKnownSize() {
        val fit = syntheticFit()
        val compressed = zlib(fit)
        assertTrue(CirqaZlibFileDecoder.isZlibCandidate(compressed))
        val decoded = CirqaZlibFileDecoder.decode(compressed)
        assertNotNull(decoded)
        assertEquals(fit.size, decoded!!.size)
        assertNotNull(FitFileHeader.inspectVerified(decoded))
    }

    @Test fun rejectsNonZlibAndTruncatedStreams() {
        val fit = syntheticFit()
        assertFalse(CirqaZlibFileDecoder.isZlibCandidate(fit))
        assertNull(CirqaZlibFileDecoder.decode(fit))

        val compressed = zlib(fit)
        val truncated = compressed.copyOf(compressed.size - 2)
        assertTrue(CirqaZlibFileDecoder.isZlibCandidate(truncated))
        assertNull(CirqaZlibFileDecoder.decode(truncated))
    }

    @Test fun normalizesFlaggedMlrDataHandle() {
        // A bare handle is returned unchanged; a flagged byte (0x80 set) carries
        // the handle in bits 6:4. This is the normalization the probe now uses
        // before comparing with the registered MLR service handle.
        assertEquals(3, GarminMlOffline.inspectDataHandle(byteArrayOf(0x03, 0, 0)))
        assertEquals(3, GarminMlOffline.inspectDataHandle(byteArrayOf(0xb0.toByte(), 0, 0)))
        assertNull(GarminMlOffline.inspectDataHandle(byteArrayOf(0x00, 0, 0)))
    }
}
