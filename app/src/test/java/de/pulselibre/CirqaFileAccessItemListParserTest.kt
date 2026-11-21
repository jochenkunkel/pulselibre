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

    private fun scalarField(number: Int, value: Long): ByteArray =
        varint(number.toLong() shl 3) + varint(value)

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

    @Test fun redactedSummaryClassifiesKnownFitTypesWithoutUid() {
        val uid = ByteArray(16) { (it + 1).toByte() }
        val typeText = "FIT_TYPE_72".toByteArray(Charsets.UTF_8)
        val dataType = byteArrayOf(0x08, 0x01) + field(2, typeText)
        val ref = field(1, uid) + field(2, dataType) +
            java.io.ByteArrayOutputStream().apply { write(0x18); write(varint(4321)) }.toByteArray()
        val item = CirqaFileAccessItemListParser.parse(field(43, field(10, field(4, ref)))).single()
        assertEquals("FIT_TYPE_72", CirqaFileAccessItemListParser.safeTypeLabel(item))
        val summary = CirqaFileAccessItemListParser.redactedSummary(listOf(item))
        assertTrue(summary.contains("FIT_TYPE_72:1"))
        assertFalse(summary.contains(uid.joinToString("") { "%02x".format(it) }))
    }

    @Test fun parsesPageContextValuesWithoutLoggingOpaqueIds() {
        val response = scalarField(1, 0) + scalarField(2, 7) + scalarField(3, 42)
        val smart = field(43, field(10, response))
        val page = CirqaFileAccessItemListParser.parsePage(smart)!!
        assertEquals(0L, page.status)
        assertEquals(7L, page.sessionId)
        assertEquals(42L, page.nextTransactionId)
        assertTrue(page.items.isEmpty())
    }

    @Test fun nextTransactionIdDoesNotContinueCurrentHistoricalListing() {
        val page = CirqaFileAccessItemListParser.ItemListPage(
            items = emptyList(),
            status = 0,
            sessionId = null,
            nextTransactionId = 42,
            gdxmlStrings = emptyMap()
        )
        assertNull(CirqaFileAccessListRequest.nextPageSessionId(page))
    }

    @Test fun nextTransactionProbeBuildsOneSeparateTransactionContextRequest() {
        val page = CirqaFileAccessItemListParser.ItemListPage(
            items = emptyList(),
            status = 0,
            sessionId = null,
            nextTransactionId = 42,
            gdxmlStrings = emptyMap()
        )
        assertArrayEquals(
            CirqaFileAccessListRequest.mlValue(handle = 7, requestId = 9, transactionId = 42),
            CirqaFileAccessListRequest.nextTransactionMlValue(7, 9, page)
        )
        assertNull(
            CirqaFileAccessListRequest.nextTransactionMlValue(
                7, 9, page.copy(nextTransactionId = null)
            )
        )
    }

    @Test fun buildsSessionOrTransactionContinuationWithVerifiedFieldNumbers() {
        val initial = CirqaFileAccessListRequest.smartRequest()
        val session = CirqaFileAccessListRequest.smartRequest(sessionId = 0x1234)
        val transaction = CirqaFileAccessListRequest.smartRequest(transactionId = 0x5678)
        assertArrayEquals(field(43, field(9, byteArrayOf())), initial)
        assertArrayEquals(field(43, field(9, scalarField(1, 0x1234))), session)
        assertArrayEquals(field(43, field(9, scalarField(2, 0x5678))), transaction)
        try {
            CirqaFileAccessListRequest.smartRequest(sessionId = 1, transactionId = 2)
            fail("Session and transaction context must not be combined")
        } catch (_: IllegalArgumentException) { }
    }

    @Test fun resolvesGdxmlStringKeysAndKeepsLabelsSafe() {
        val uid = ByteArray(16) { (it + 10).toByte() }
        val dataType = scalarField(1, 0) + scalarField(3, 9)
        val ref = field(1, uid) + field(2, dataType) + scalarField(3, 100)
        val response = scalarField(1, 0) +
            field(4, ref) +
            scalarField(2, 22)
        val directType = scalarField(1, 0) + field(2, "FIT_TYPE_49".toByteArray()) +
            scalarField(3, 9)
        val directRef = field(1, uid) + field(2, directType) + scalarField(3, 200)
        val smart = field(43, field(10, response + field(4, directRef)))
        val page = CirqaFileAccessItemListParser.parsePage(smart)!!
        assertEquals("FIT_TYPE_49", CirqaFileAccessItemListParser.safeTypeLabel(page.items[0], page.gdxmlStrings))
        assertEquals("FIT_TYPE_49", CirqaFileAccessItemListParser.safeTypeLabel(page.items[1], page.gdxmlStrings))
        assertFalse(CirqaFileAccessItemListParser.redactedSummary(page.items).contains(
            uid.joinToString("") { "%02x".format(it) }
        ))
    }
}
