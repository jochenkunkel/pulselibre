package de.pulselibre

import org.junit.Assert.*
import org.junit.Test

class CirqaConnectDirectoryTransitionTest {
    private fun response(type: Int, outer: Int = 0, inner: Int? = 0): ByteArray {
        val body = byteArrayOf(type.toByte(), (type ushr 8).toByte(), outer.toByte()) +
            (inner?.let { byteArrayOf(it.toByte()) } ?: byteArrayOf())
        val size = body.size + 6
        val prefix = byteArrayOf(size.toByte(), 0, 0x88.toByte(), 0x13) + body
        val crc = GarminFrameValidator.crc16Arc(prefix)
        return prefix + byteArrayOf(crc.toByte(), (crc ushr 8).toByte())
    }

    private fun ready(): CirqaConnectDirectoryTransition = CirqaConnectDirectoryTransition(
        // Synthetic evidence, not a claim about the CIRQA or Connect values.
        mapOf(5030 to setOf(0), 5007 to setOf(0))
    ).apply {
        assertTrue(begin(true, true, 0))
        assertTrue(bootstrapCompleted(true, true, true, true, true, false, false, 1))
    }

    @Test fun candidateHasOnlyHandshakeEventAndVerifiedEnvelope() {
        val frame = CirqaLegacy5030RequestCandidate.gfdiFrame()
        assertEquals(8, frame.size)
        assertNotNull(GarminFrameValidator.inspectVerified(frame))
        assertEquals(5030, GarminGfdiHeader.inspect(frame)?.type)
        assertArrayEquals(byteArrayOf(8, 0), frame.copyOfRange(4, 6))
    }

    @Test fun requiresSeparatePersistedAuthorizationAndCachedTypes() {
        for ((persisted, types) in listOf(false to false, false to true, true to false)) {
            val model = CirqaConnectDirectoryTransition()
            assertFalse(model.begin(persisted, types, 0))
            assertFalse(model.begin(true, true, 1))
            assertEquals(CirqaConnectDirectoryTransition.Stage.STOPPED, model.stage)
        }
    }

    @Test fun everyBootstrapRequirementIsMandatory() {
        for (missing in 0..6) {
            val m = CirqaConnectDirectoryTransition()
            assertTrue(m.begin(true, true, 0))
            val flags = BooleanArray(7) { it < 5 }
            flags[missing] = !flags[missing]
            assertFalse(m.bootstrapCompleted(
                flags[0], flags[1], flags[2], flags[3], flags[4], flags[5], flags[6], 1
            ))
            assertEquals(CirqaConnectDirectoryTransition.Stage.BOOTSTRAP, m.stage)
        }
    }

    @Test fun requiresBothGattCompletionAndInnerSuccessInEitherOrder() {
        for (responseFirst in listOf(false, true)) {
            val m = ready()
            assertTrue(m.claimSystemEvent(2))
            if (responseFirst) {
                assertTrue(m.response(response(5030), 3))
            } else {
                assertTrue(m.writeCompleted(5030, true, 3))
            }
            assertEquals(CirqaConnectDirectoryTransition.Stage.SYSTEM_EVENT_PENDING, m.stage)
            if (responseFirst) {
                assertTrue(m.writeCompleted(5030, true, 4))
            } else {
                assertTrue(m.response(response(5030), 4))
            }
            assertEquals(CirqaConnectDirectoryTransition.Stage.FILTER_READY, m.stage)
            assertTrue(m.claimFilter(5))
            assertTrue(m.response(response(5007), 6))
            assertEquals(CirqaConnectDirectoryTransition.Stage.FILTER_PENDING, m.stage)
            assertTrue(m.writeCompleted(5007, true, 7))
            assertTrue(m.claimDirectory(8))
            assertFalse(m.claimDirectory(9))
        }
    }

    @Test fun rejectsOuterOnlyAckWrongTypeNegativeStatusMalformedCrcAndExtraPayload() {
        val extra = response(5030) + byteArrayOf(0)
        val cases = listOf(
            response(5030, inner = null), response(5007),
            response(5030, outer = 2), response(5030, inner = 1),
            response(5030).also { it[it.lastIndex] = (it.last().toInt() xor 1).toByte() },
            extra
        )
        for (frame in cases) {
            val m = ready()
            assertTrue(m.claimSystemEvent(2))
            assertFalse(m.response(frame, 3))
            assertFalse(m.claimFilter(4))
            assertFalse(m.claimDirectory(5))
        }
    }

    @Test fun cannotSkipOrRepeatSystemEventOrReleaseOnFailedWrite() {
        val early = ready()
        assertFalse(early.claimFilter(2))
        val duplicate = ready()
        assertTrue(duplicate.claimSystemEvent(2))
        assertFalse(duplicate.claimSystemEvent(3))
        val failed = ready()
        assertTrue(failed.claimSystemEvent(2))
        assertTrue(failed.response(response(5030), 3))
        assertFalse(failed.writeCompleted(5030, false, 4))
        assertFalse(failed.claimFilter(5))
    }

    @Test fun deadlineAbortAndDuplicatesRemainTerminal() {
        val timeout = ready()
        assertFalse(timeout.checkDeadline(CirqaConnectDirectoryTransition.MAX_SESSION_MILLIS))
        assertEquals("timeout", timeout.stopReason)
        assertFalse(timeout.claimSystemEvent(2))
        assertEquals("timeout", timeout.stopReason)
        val aborted = ready()
        assertFalse(aborted.abort())
        assertFalse(aborted.claimSystemEvent(2))
        val duplicate = ready()
        assertTrue(duplicate.claimSystemEvent(2))
        assertTrue(duplicate.response(response(5030), 3))
        assertFalse(duplicate.response(response(5030), 4))
        val wrongCallback = ready()
        assertTrue(wrongCallback.claimSystemEvent(2))
        assertFalse(wrongCallback.writeCompleted(5007, true, 3))
    }

    @Test fun refusesUnclaimedResponsesDuplicateCallbacksAndInvalidClocks() {
        val unclaimed = ready()
        assertFalse(unclaimed.response(response(5030), 2))
        val duplicate = ready()
        assertTrue(duplicate.claimSystemEvent(2))
        assertTrue(duplicate.writeCompleted(5030, true, 3))
        assertFalse(duplicate.writeCompleted(5030, true, 4))
        for (clock in listOf(-1L, Long.MAX_VALUE)) {
            assertFalse(CirqaConnectDirectoryTransition().begin(true, true, clock))
        }
        val backdated = CirqaConnectDirectoryTransition()
        assertTrue(backdated.begin(true, true, 100))
        assertFalse(backdated.checkDeadline(99))
    }

    @Test fun productionDefaultDoesNotGuessSuccessForAnyByte() {
        for (value in 0..255) {
            val m = CirqaConnectDirectoryTransition()
            assertTrue(m.begin(true, true, 0))
            assertTrue(m.bootstrapCompleted(true, true, true, true, true, false, false, 1))
            assertTrue(m.claimSystemEvent(2))
            assertTrue(m.writeCompleted(5030, true, 3))
            assertFalse(m.response(response(5030, inner = value), 4))
            assertFalse(m.claimFilter(5))
        }
    }
}
