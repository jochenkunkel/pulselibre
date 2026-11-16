package de.pulselibre

import org.junit.Assert.*
import org.junit.Test

class CirqaBootstrapStateMachineTest {
    private fun frame(type: Int, body: ByteArray): ByteArray {
        val size = body.size + 6
        val prefix = byteArrayOf(size.toByte(), (size ushr 8).toByte(),
            type.toByte(), (type ushr 8).toByte()) + body
        val crc = GarminFrameValidator.crc16Arc(prefix)
        return prefix + byteArrayOf(crc.toByte(), (crc ushr 8).toByte())
    }

    @Test fun identifiesFiveSecondRetriesWithoutInventingAck() {
        val machine = CirqaBootstrapStateMachine()
        machine.observe(frame(5024, byteArrayOf(1)), 1540)
        assertEquals(CirqaBootstrapStateMachine.State.DEVICE_INFORMATION_SEEN, machine.state)
        machine.onAckAttempt()
        val msg = frame(5050, byteArrayOf(7, 8))
        machine.observe(msg, 1599)
        machine.observe(msg, 6597)
        machine.observe(msg, 11604)
        machine.observe(frame(5011, byteArrayOf(9)), 16620)
        assertEquals(2, machine.retries)
        assertEquals(CirqaBootstrapStateMachine.State.POST_RETRY_5011_SEEN, machine.state)
        assertTrue(machine.summary().contains("4998, 5007"))
        assertTrue(machine.summary().contains("Anwendungs-ACK nachgewiesen=false"))
    }
}
