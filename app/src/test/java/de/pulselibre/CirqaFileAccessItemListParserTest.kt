package de.pulselibre

import org.junit.Assert.*
import org.junit.Test

class CirqaFileAccessItemListParserTest {
    private fun field(number: Int, bytes: ByteArray): ByteArray {
        val out = java.io.ByteArrayOutputStream()
        out.write(varint((number.toLong() shl 3) or 2))
        out.write(varint(bytes.size.toLong()))
        out.write(bytes)
        return out.toByteArray()
    }

    private fun varint(value: Long): ByteArray {
        var v = value
        val out = java.io.ByteArrayOutputStream()
        do {
            val b = (v and 127).toInt()
            v = v ushr 7
            out.write(if (v == 0L) b else b or 128)
        } while (v != 0L)
        return out.toByteArray()
    }

    @Test fun parsesItemListFromSmartEnvelope() {
        val uid = ByteArray(16) { (it + 1).toByte() }
        val dataType = byteArrayOf(0x08, 0x01)
        val ref = field(1, uid) + field(2, dataType) +
            java.io.ByteArrayOutputStream().apply { write(0x18); write(varint(4321)) }.toByteArray()
        val response = field(4, ref)
        val service = field(10, response)
        val smart = field(43, service)
        val items = CirqaFileAccessItemListParser.parse(smart)
        assertEquals(1, items.size)
        assertArrayEquals(uid, items[0].uid)
        assertEquals(4321L, items[0].dataSize)
        assertTrue(CirqaFileAccessItemListParser.redactedSummary(items).contains("4321"))
        assertFalse(CirqaFileAccessItemListParser.redactedSummary(items).contains("-"))
    }

    @Test fun pullRequestRoundTripsThroughParser() {
        val item = CirqaFileAccessItemListParser.Item(
            uid = ByteArray(16) { (it + 3).toByte() },
            dataType = byteArrayOf(0x08, 0x02),
            dataSize = 777,
            flags = 0
        )
        val smart = CirqaFileAccessPullRequest.smart(item)
        val frame = CirqaFileAccessPullRequest.gfdiFrame(5, smart)
        assertNotNull(GarminFrameValidator.inspectVerified(frame))
        assertEquals(5043, GarminGfdiHeader.inspect(frame)!!.type)
        val body = frame.copyOfRange(4, frame.size - 2)
        assertArrayEquals(smart, body.copyOfRange(14, body.size))
    }
}
