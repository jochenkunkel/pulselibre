package de.pulselibre

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** Byte vectors from the Connect reference run of 28.11.2026 (#737); no identifiers. */
class CirqaActivityStatusTest {
    private fun hex(s: String) = s.chunked(2).map { it.toInt(16).toByte() }.toByteArray()
    // Smart{30:{3: <update>}}: tag F2 01, len, then 1A len <update>
    private fun smart30(field: Int, inner: String): ByteArray {
        val body = hex(inner)
        val sub = byteArrayOf(((field shl 3) or 2).toByte(), body.size.toByte()) + body
        return byteArrayOf(0xF2.toByte(), 0x01, sub.size.toByte()) + sub
    }

    @Test fun subscriptionIsTheConnectRequest() {
        assertEquals("f201060a040a020813", CirqaActivityStatus.subscribe().joinToString("") { "%02x".format(it) })
    }

    @Test fun stateCodesFromObservedMessages() {
        assertEquals(3, CirqaActivityStatus.stateCode(smart30(3, "0a0113a23f0412020803")))
        assertEquals(1, CirqaActivityStatus.stateCode(smart30(3, "0a0113a23f0412020801")))
        assertEquals(5, CirqaActivityStatus.stateCode(smart30(3, "0a0113a23f0412020805")))
        assertEquals(4, CirqaActivityStatus.stateCode(smart30(3, "0a0113a23f06120408041000")))
        assertEquals(CirqaActivityStatus.State.RUNNING, CirqaActivityStatus.state(3))
        assertEquals(CirqaActivityStatus.State.UNKNOWN, CirqaActivityStatus.state(9))
    }

    @Test fun statisticsAndOtherServicesHaveNoState() {
        // per-second statistics: container field 3 instead of 2
        assertNull(CirqaActivityStatus.stateCode(smart30(3, "0a0113a23f041a020800")))
        assertNull(CirqaActivityStatus.stateCode(CirqaMonitoringFlushRequest.smart()))
    }

    @Test fun subscriptionAnswerIsRecognised() {
        assertTrue(CirqaActivityStatus.isSubscribed(smart30(2, "0a06080012020813")))
        assertFalse(CirqaActivityStatus.isSubscribed(smart30(3, "0a0113a23f0412020803")))
    }

    @Test fun endCommandsAreTheConnectBytes() {
        // host 5043 payloads from the Connect reference run, 11:48:14 and 11:48:15
        assertEquals("c203058a01021002", CirqaActivityStatus.command(CirqaActivityStatus.STOP).joinToString("") { "%02x".format(it) })
        assertEquals("c203058a01021003", CirqaActivityStatus.command(CirqaActivityStatus.SAVE).joinToString("") { "%02x".format(it) })
    }

    @Test fun endSequenceIsStopThenSaveOnce() {
        assertEquals(CirqaActivityStatus.STOP, CirqaActivityStatus.nextEndCommand(3, stopSent = false, saveSent = false))
        assertNull(CirqaActivityStatus.nextEndCommand(3, stopSent = true, saveSent = false))
        assertEquals(CirqaActivityStatus.STOP, CirqaActivityStatus.nextEndCommand(9, stopSent = false, saveSent = false))
        assertEquals(CirqaActivityStatus.SAVE, CirqaActivityStatus.nextEndCommand(1, stopSent = true, saveSent = false))
        assertNull(CirqaActivityStatus.nextEndCommand(1, stopSent = true, saveSent = true))
        assertNull(CirqaActivityStatus.nextEndCommand(4, stopSent = true, saveSent = true))
        assertNull(CirqaActivityStatus.nextEndCommand(5, stopSent = false, saveSent = false))
        assertNull(CirqaActivityStatus.nextEndCommand(null, stopSent = false, saveSent = false))
    }
}
