package de.pulselibre

import org.junit.Assert.assertEquals
import org.junit.Test

class CirqaConnect531HandshakeModelTest {
    @Test fun ackAndGattWriteDoNotCompleteHandshake() {
        val m = CirqaConnect531HandshakeModel()
        assertEquals(CirqaConnect531HandshakeModel.Decision.ACK_5024, m.deviceInformation(true))
        assertEquals(CirqaConnect531HandshakeModel.Decision.ACK_5050_AND_PREPARE_HOST_RESPONSE,
            m.deviceConfiguration(byteArrayOf(1, 8)))
        m.gattWriteConfirmed()
        m.gfdi5050Ack()
        assertEquals(CirqaConnect531HandshakeModel.State.HOST_RESPONSE_PENDING, m.state)
        m.hostResponseCompleted(true)
        assertEquals(CirqaConnect531HandshakeModel.State.COMPLETE, m.state)
    }

    @Test fun invalid5050DoesNotAdvance() {
        val m = CirqaConnect531HandshakeModel()
        assertEquals(CirqaConnect531HandshakeModel.Decision.IGNORE, m.deviceConfiguration(byteArrayOf(0)))
        m.deviceInformation(true)
        assertEquals(CirqaConnect531HandshakeModel.Decision.LENGTH_ERROR,
            m.deviceConfiguration(byteArrayOf(2, 1)))
        assertEquals(CirqaConnect531HandshakeModel.State.WAIT_5050, m.state)
    }

    @Test fun failedHostResponseDoesNotComplete() {
        val m = CirqaConnect531HandshakeModel()
        m.deviceInformation(true)
        m.deviceConfiguration(byteArrayOf(0))
        m.hostResponseCompleted(false)
        assertEquals(CirqaConnect531HandshakeModel.State.FAILED, m.state)
    }

    @Test fun no5030EventIsRequired() {
        val m = CirqaConnect531HandshakeModel()
        m.deviceInformation(true)
        m.deviceConfiguration(byteArrayOf(0))
        m.hostResponseCompleted(true)
        assertEquals(CirqaConnect531HandshakeModel.State.COMPLETE, m.state)
    }
}
