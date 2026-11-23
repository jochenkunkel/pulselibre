package de.pulselibre

import org.junit.Assert.*
import org.junit.Test

class CirqaFileAccessItemListParserTest {
    @Test fun preservesRemoteRevisionAndUnknownReferenceFieldsWithoutGrantingASkip() {
        val uid = byteArrayOf(1, 2, 3, 4)
        val type = byteArrayOf(0x08, 0x01)
        fun reference(transaction: Long, modified: Long) = field(1, uid) + field(2, type) +
            scalarField(3, 128) + scalarField(5, transaction) + scalarField(7, modified) +
            field(12, byteArrayOf(8, 7, 6))
        fun parse(ref: ByteArray) =
            CirqaFileAccessItemListParser.parse(field(43, field(10, scalarField(1, 0) + field(4, ref)))).single()
        val before = parse(reference(10, 1000))
        val afterBytes = reference(11, 2000)
        val after = parse(afterBytes)
        assertEquals(before.dataSize, after.dataSize)
        assertArrayEquals(before.uid, after.uid)
        assertEquals(11L, after.transactionId)
        assertEquals(2000L, after.modifiedTime)
        assertArrayEquals(afterBytes, after.referenceBytes)
        val identity = CirqaTargetedPullPolicy.ItemIdentity("synthetic-uid", "fit", 128, 0)
        val evidence = CirqaUidStabilityEvidence.from(listOf("s1", "s2", "s3").map {
            CirqaUidStabilityEvidence.SessionObservation(
                it, true, true, CirqaTargetedPullPolicy.stabilityKey(identity))
        })
        assertTrue(CirqaTargetedPullPolicy.shouldPull(identity, listOf(
            CirqaTargetedPullPolicy.CompletedImport(identity, "a".repeat(64), true, evidence))))
    }

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

    @Test fun parsesRedactedPullErrorMetadataIncludingSignedCodes() {
        fun sint32(value: Int): Long =
            ((value shl 1) xor (value shr 31)).toLong() and 0xffff_ffffL

        val response = scalarField(1, 3) + scalarField(2, 0) +
            scalarField(5, sint32(-7)) + scalarField(6, 2) +
            scalarField(8, 4) + scalarField(9, sint32(11))
        val smart = field(43, field(2, response))

        val parsed = CirqaFileAccessItemListParser.parsePullResult(smart)!!
        assertEquals(3L, parsed.resultCode)
        assertEquals(0L, parsed.transport)
        assertEquals(0L, parsed.transferHandle)
        assertEquals(-7, parsed.errorCode)
        assertEquals(2L, parsed.errorSource)
        assertEquals(4L, parsed.dataSpecificErrorSource)
        assertEquals(11, parsed.dataSpecificErrorCode)
        assertFalse(parsed.hasFilePath)
    }

    @Test fun omittedPullErrorMetadataRemainsAbsent() {
        val parsed = CirqaFileAccessItemListParser.parsePullResult(
            field(43, field(2, scalarField(1, 0)))
        )!!

        assertNull(parsed.errorCode)
        assertNull(parsed.errorSource)
        assertNull(parsed.dataSpecificErrorSource)
        assertNull(parsed.dataSpecificErrorCode)
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

    private fun declaredLengthField(number: Int, declaredLength: Long, content: ByteArray): ByteArray {
        val key = varint((number.toLong() shl 3) or 2)
        val out = java.io.ByteArrayOutputStream()
        out.write(key)
        out.write(varint(declaredLength))
        out.write(content)
        return out.toByteArray()
    }

    @Test fun truncatedItemListResponseDoesNotYieldPartialPage() {
        val ref = field(1, ByteArray(16)) + field(2, byteArrayOf(0x08, 0x01)) + scalarField(3, 100)
        val response = declaredLengthField(4, 10_000, ref)
        val smart = field(43, field(10, response))
        assertNull(CirqaFileAccessItemListParser.parsePage(smart))
        assertTrue(CirqaFileAccessItemListParser.parse(smart).isEmpty())
    }

    @Test fun oversizedItemListLengthIsRejected() {
        val response = declaredLengthField(4, 2_147_483_648L, ByteArray(0)) // 2^31 exceeds Int range
        val smart = field(43, field(10, response))
        assertNull(CirqaFileAccessItemListParser.parsePage(smart))
    }

    @Test fun fieldCountAboveCanonicalLimitIsRejected() {
        val out = java.io.ByteArrayOutputStream()
        repeat(513) { index ->
            out.write(varint((index + 1).toLong() shl 3))
            out.write(varint(0))
        }
        assertNull(CirqaFileAccessItemListParser.fields(out.toByteArray()))
    }

    @Test fun fieldCountAtCanonicalLimitIsAccepted() {
        val out = java.io.ByteArrayOutputStream()
        repeat(512) { index ->
            out.write(varint((index + 1).toLong() shl 3))
            out.write(varint(0))
        }
        assertEquals(512, CirqaFileAccessItemListParser.fields(out.toByteArray())?.size)
    }

    @Test fun unsupportedWireTypeIsRejected() {
        val bytes = varint((10L shl 3) or 7) + varint(0)
        assertNull(CirqaFileAccessItemListParser.fields(bytes))
    }

    @Test fun zeroFieldNumberIsRejected() {
        assertNull(CirqaFileAccessItemListParser.fields(varint(0) + varint(0)))
    }

    @Test fun itemWithoutUidFailsClosed() {
        val ref = field(2, byteArrayOf(0x08, 0x01)) + scalarField(3, 100)
        val smart = field(43, field(10, field(4, ref)))
        assertNull(CirqaFileAccessItemListParser.parsePage(smart))
    }

    @Test fun outOfRangeSessionAndTransactionContextFailClosed() {
        val response = scalarField(1, 0) + scalarField(2, 4_294_967_296L) // 2^32 exceeds protocol max
        val smart = field(43, field(10, response))
        assertNull(CirqaFileAccessItemListParser.parsePage(smart))
    }

    @Test fun pullResponseWithoutResultFieldIsRejected() {
        val parsed = CirqaFileAccessItemListParser.parsePullResult(field(43, field(2, byteArrayOf())))
        assertNull(parsed)
    }
}
