package de.pulselibre

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CirqaLegacy5007BootstrapGateTest {
    private fun eligible(
        infoAckSent: Boolean = true,
        infoAckGatt: Boolean = true,
        configQueued: Boolean = true,
        configGatt: Boolean = true,
        configAck: Boolean = true,
        writePending: Boolean = false,
        chunksRemain: Boolean = false
    ) = CirqaLegacy5007BootstrapGate.maySend5007(
        infoAckSent, infoAckGatt, configQueued, configGatt, configAck,
        writePending, chunksRemain
    )

    @Test fun requiresEveryBootstrapAcknowledgementAndCompletedWrite() {
        assertTrue(eligible())
        assertFalse(eligible(infoAckSent = false))
        assertFalse(eligible(infoAckGatt = false))
        assertFalse(eligible(configQueued = false))
        assertFalse(eligible(configGatt = false))
        assertFalse(eligible(configAck = false))
        assertFalse(eligible(writePending = true))
        assertFalse(eligible(chunksRemain = true))
    }
}
