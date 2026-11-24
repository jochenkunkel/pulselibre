package de.pulselibre

import org.junit.Assert.*
import org.junit.Test

class CirqaFileAccessListRequestTest {
    @Test fun wrapsSmartFileAccessRequestInValid5043Transport() {
        val smart = GarminFileListPagination.smartRequest()
        val frame = CirqaFileAccessListRequest.gfdiFrame(0x1234, smart)
        assertNotNull(GarminFrameValidator.inspectVerified(frame))
        val h = GarminGfdiHeader.inspect(frame)!!
        assertEquals(5043, h.type)
        val body = frame.copyOfRange(4, frame.size - 2)
        assertEquals(0x1234, (body[0].toInt() and 255) or ((body[1].toInt() and 255) shl 8))
        assertEquals(0L, u32(body, 2))
        assertEquals(smart.size.toLong(), u32(body, 6))
        assertEquals(smart.size.toLong(), u32(body, 10))
        assertArrayEquals(smart, body.copyOfRange(14, body.size))
    }

    @Test fun smartRequestUsesFileAccessField43AndItemListField9() {
        // Smart{ field43 = Service{ field9 = empty ItemListRequest } }
        assertArrayEquals(byteArrayOf(0xda.toByte(), 0x02, 0x02, 0x4a, 0x00),
            GarminFileListPagination.smartRequest())
    }

    @Test fun transactionFollowupSerializesOnlyObservedTokenUntilFilterContextIsProven() {
        // Synthetic token only. Do not infer Connect's agent-specific filter UUIDs.
        assertArrayEquals(
            byteArrayOf(0xda.toByte(), 0x02, 0x04, 0x4a, 0x02, 0x10, 0x2a),
            CirqaFileAccessListRequest.smartRequest(transactionId = 42)
        )
    }

    @Test fun sessionContinuationSerializesSessionWithoutTransactionOrFilters() {
        assertArrayEquals(
            byteArrayOf(0xda.toByte(), 0x02, 0x04, 0x4a, 0x02, 0x08, 0x09),
            CirqaFileAccessListRequest.smartRequest(sessionId = 9)
        )
    }

    private fun u32(bytes: ByteArray, i: Int): Long =
        (0..3).fold(0L) { n, k -> n or ((bytes[i + k].toLong() and 255L) shl (8 * k)) }
}
