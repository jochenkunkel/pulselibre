package de.pulselibre

import org.junit.Assert.*
import org.junit.Test

class CirqaLegacy5030OnlyTest {
    @Test fun requestGoldenVectorIncludesBothCrcAndCobsDelimiters() {
        val expected = byteArrayOf(8, 0, 0xa6.toByte(), 0x13, 8, 0, 0xd5.toByte(), 0xc5.toByte())
        assertArrayEquals(expected, CirqaLegacy5030RequestCandidate.gfdiFrame())
        assertArrayEquals(byteArrayOf(0, 2, 8, 4, 0xa6.toByte(), 0x13, 8, 3,
            0xd5.toByte(), 0xc5.toByte(), 0), GarminFileListRequest.cobs(expected))
    }
    private fun frame(outer: Int = 0, inner: ByteArray = byteArrayOf(0), type: Int = 5030): ByteArray {
        val prefix = byteArrayOf((9 + inner.size).toByte(), 0, 0x88.toByte(), 0x13,
            type.toByte(), (type ushr 8).toByte(), outer.toByte()) + inner
        val crc = GarminFrameValidator.crc16Arc(prefix)
        return prefix + byteArrayOf(crc.toByte(), (crc ushr 8).toByte())
    }

    private fun waiting(): CirqaLegacy5030OnlySession = CirqaLegacy5030OnlySession().apply {
        assertTrue(begin(true, 0))
        assertTrue(bootstrapReady(token, true, 1))
        assertTrue(claimRequest(token, 2))
    }

    @Test fun retainsOuterStatusLengthAndSignedFirstResultWithoutGuessingMeaning() {
        for (value in 0..255) {
            val meta = CirqaLegacy5030Response.inspect(frame(inner = byteArrayOf(value.toByte())))!!
            assertEquals(value, meta.firstResultUnsigned)
            assertEquals(value.toByte().toInt(), meta.connectResult)
        }
        val empty = CirqaLegacy5030Response.inspect(frame(inner = byteArrayOf()))!!
        assertEquals(0, empty.payloadLength)
        assertNull(empty.firstResultUnsigned)
        assertEquals(-1, empty.connectResult)
        assertEquals(2, CirqaLegacy5030Response.inspect(frame(2))?.outerStatus)
        assertNull(CirqaLegacy5030Response.inspect(frame(type = 5007)))
    }

    @Test fun fragmentedResponseAndWriteInBothOrdersStopWithoutFollowUp() {
        val bytes = GarminFileListRequest.cobs(frame())
        for (responseFirst in listOf(false, true)) {
            val s = waiting()
            if (!responseFirst) assertTrue(s.writeCompleted(s.token, true, 3))
            bytes.forEachIndexed { i, b -> assertTrue(s.receive(s.token, byteArrayOf(b), 4L + i)) }
            if (responseFirst) {
                assertEquals(CirqaLegacy5030OnlySession.Phase.WAITING, s.phase)
                assertTrue(s.writeCompleted(s.token, true, 100))
            }
            assertEquals(CirqaLegacy5030OnlySession.Phase.OBSERVED, s.phase)
            assertTrue(s.summary().contains("innerMeaning=unproven"))
            assertFalse(s.claimRequest(s.token, 101))
            assertFalse(s.receive(s.token, bytes, 102))
        }
    }

    @Test fun outerOnlyAndUnknownInnerRemainObservationsNotSuccess() {
        for (inner in listOf(byteArrayOf(), byteArrayOf(1), byteArrayOf(0, 0))) {
            val s = waiting()
            assertTrue(s.writeCompleted(s.token, true, 3))
            assertTrue(s.receive(s.token, GarminFileListRequest.cobs(frame(inner = inner)), 4))
            assertEquals(CirqaLegacy5030OnlySession.Phase.OBSERVED, s.phase)
            assertEquals(inner.size, s.metadata?.payloadLength)
        }
    }

    @Test fun negativeUnexpectedMalformedOversizeAndMultipleResponsesStop() {
        val badCrc = frame().also { it[it.lastIndex] = (it.last().toInt() xor 1).toByte() }
        val cases = listOf(
            GarminFileListRequest.cobs(frame(outer = 2)),
            GarminFileListRequest.cobs(frame(type = 5007)),
            GarminFileListRequest.cobs(badCrc),
            byteArrayOf(0, 4, 1, 0),
            byteArrayOf(1, 2),
            byteArrayOf(0) + ByteArray(67) { 1 },
            GarminFileListRequest.cobs(frame()) + GarminFileListRequest.cobs(frame())
        )
        for (bytes in cases) {
            val s = waiting()
            assertFalse(s.receive(s.token, bytes, 3))
            assertEquals(CirqaLegacy5030OnlySession.Phase.STOPPED, s.phase)
            assertFalse(s.writeCompleted(s.token, true, 4))
        }
    }

    @Test fun sessionTokensRejectLateCallbacksWithoutChangingCurrentSession() {
        val old = waiting()
        val fresh = waiting()
        assertFalse(fresh.writeCompleted(old.token, true, 3))
        assertFalse(fresh.receive(old.token, GarminFileListRequest.cobs(frame()), 3))
        assertFalse(fresh.abort(old.token))
        assertEquals(CirqaLegacy5030OnlySession.Phase.WAITING, fresh.phase)
        assertTrue(fresh.writeCompleted(fresh.token, true, 3))
    }

    @Test fun timeoutDisconnectDuplicateCallbacksAndGateDenialCannotRestart() {
        val timeout = waiting()
        assertFalse(timeout.tick(timeout.token, 5002))
        assertFalse(timeout.begin(true, 6000))
        val noBootstrap = CirqaLegacy5030OnlySession()
        assertTrue(noBootstrap.begin(true, 0))
        assertFalse(noBootstrap.tick(noBootstrap.token, 30_000))
        val duplicate = waiting()
        assertTrue(duplicate.writeCompleted(duplicate.token, true, 3))
        assertFalse(duplicate.writeCompleted(duplicate.token, true, 4))
        val disconnected = waiting()
        assertFalse(disconnected.abort(disconnected.token))
        assertFalse(disconnected.receive(disconnected.token, GarminFileListRequest.cobs(frame()), 3))
        val denied = CirqaLegacy5030OnlySession()
        assertFalse(denied.begin(false, 0))
        assertFalse(denied.begin(true, 1))
    }

    @Test fun clockRegressionAndGlobalByteBudgetStop() {
        val s = waiting()
        assertTrue(s.tick(s.token, 10))
        assertFalse(s.tick(s.token, 9))
        val budget = waiting()
        assertFalse(budget.receive(budget.token, ByteArray(257), 3))
    }

    @Test fun authorizedGateIsStillAtomicOneShotAndFailsClosedOnPersistenceFailure() {
        assertTrue(CirqaLegacy5030AttemptGate.RELEASED_FOR_DEVICE_TEST)
        var consumed = false
        var writes = 0
        fun attempt(released: Boolean, confirmed: Boolean, persistOk: Boolean) =
            CirqaLegacy5030AttemptGate.consume(released, confirmed, { consumed }, {
                writes++
                if (persistOk) consumed = true
                persistOk
            })
        assertFalse(attempt(false, true, true))
        assertFalse(attempt(true, false, true))
        assertEquals(0, writes)
        assertFalse(attempt(true, true, false))
        assertFalse(consumed)
        assertTrue(attempt(true, true, true))
        assertFalse(attempt(true, true, true))
        assertEquals(2, writes)
    }
}
