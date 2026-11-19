package de.pulselibre

import org.junit.Assert.*
import org.junit.Test

class RecorderAttemptGateTest {
    @Test fun successBeforeLateCallbackIsTerminalExactlyOnce() {
        val gate = RecorderAttemptGate()
        val owner = Any()
        val token = gate.start()
        assertTrue(gate.attach(token, owner))
        assertTrue(gate.accepts(token, owner))
        val retry = gate.finish(token, owner)
        assertNotNull(retry)
        assertNull(gate.finish(token, owner))
        assertFalse(gate.accepts(token, owner))
        assertTrue(gate.canRetry(retry!!))
        assertFalse(gate.canRetry(token))
    }

    @Test fun lateReadingAndDuplicateEndFromPreviousProbeAreRejected() {
        val gate = RecorderAttemptGate()
        val old = Any()
        val first = gate.start()
        assertTrue(gate.attach(first, old))
        val next = gate.finish(first, old)!!
        assertTrue(gate.canRetry(next))
        val newToken = gate.start()
        val replacement = Any()
        assertTrue(gate.attach(newToken, replacement))
        assertFalse(gate.accepts(first, old))
        assertFalse(gate.accepts(newToken, old))
        assertNull(gate.finish(first, old))
        assertTrue(gate.accepts(newToken, replacement))
    }

    @Test fun stopInvalidatesQueuedCallbacksAndRetry() {
        val gate = RecorderAttemptGate()
        val owner = Any()
        val token = gate.start()
        assertTrue(gate.attach(token, owner))
        gate.stop()
        assertFalse(gate.accepts(token, owner))
        assertNull(gate.finish(token, owner))
        assertFalse(gate.canRetry(token))
    }

    @Test fun serviceRestartInvalidatesOldGeneration() {
        val gate = RecorderAttemptGate()
        val old = Any()
        val first = gate.start()
        assertTrue(gate.attach(first, old))
        gate.stop()
        val second = gate.start()
        assertTrue(gate.attach(second, Any()))
        assertFalse(gate.accepts(first, old))
        assertNull(gate.finish(first, old))
    }

    @Test fun bluetoothOffAllowsOnlyCurrentUnattachedRetry() {
        val gate = RecorderAttemptGate()
        val first = gate.start()
        assertTrue(gate.canRetry(first))
        val second = gate.start()
        assertFalse(gate.canRetry(first))
        assertTrue(gate.canRetry(second))
        gate.stop()
        assertFalse(gate.canRetry(second))
    }

    @Test fun wrongOwnerAndDoubleAttachmentAreRejected() {
        val gate = RecorderAttemptGate()
        val token = gate.start()
        val owner = Any()
        assertTrue(gate.attach(token, owner))
        assertFalse(gate.attach(token, Any()))
        assertFalse(gate.accepts(token, Any()))
        assertNull(gate.finish(token, Any()))
        assertTrue(gate.accepts(token, owner))
    }
}
