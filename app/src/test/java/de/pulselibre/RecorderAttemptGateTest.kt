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

class RecorderBackoffTest {
    @org.junit.Test fun backoffDoublesThenCapsAtSixtySeconds() {
        assertEquals(listOf(2_000L, 4_000L, 8_000L, 16_000L, 32_000L, 60_000L, 60_000L),
            (1..7).map(RecorderBackoff::delayMs))
    }

    @org.junit.Test fun backoffIsBoundedForOutOfRangeAttemptNumbers() {
        assertEquals(2_000L, RecorderBackoff.delayMs(0))
        assertEquals(2_000L, RecorderBackoff.delayMs(-5))
        assertEquals(60_000L, RecorderBackoff.delayMs(Int.MAX_VALUE))
    }
}

class RecorderBluetoothPolicyTest {
    @org.junit.Test fun bluetoothTurningOffOrOffAbandonsTheDeadAttempt() {
        assertEquals(RecorderBluetoothPolicy.Action.ABANDON_ATTEMPT, RecorderBluetoothPolicy.actionFor(android.bluetooth.BluetoothAdapter.STATE_TURNING_OFF))
        assertEquals(RecorderBluetoothPolicy.Action.ABANDON_ATTEMPT, RecorderBluetoothPolicy.actionFor(android.bluetooth.BluetoothAdapter.STATE_OFF))
    }

    @org.junit.Test fun bluetoothOnRestartsTheAttemptImmediately() {
        assertEquals(RecorderBluetoothPolicy.Action.RESTART_ATTEMPT, RecorderBluetoothPolicy.actionFor(android.bluetooth.BluetoothAdapter.STATE_ON))
    }

    @org.junit.Test fun transitionalAndUnknownStatesDoNothing() {
        assertEquals(RecorderBluetoothPolicy.Action.NONE, RecorderBluetoothPolicy.actionFor(android.bluetooth.BluetoothAdapter.STATE_TURNING_ON))
        assertEquals(RecorderBluetoothPolicy.Action.NONE, RecorderBluetoothPolicy.actionFor(android.bluetooth.BluetoothAdapter.ERROR))
    }

    @org.junit.Test fun literalsMatchTheAndroidConstants() {
        assertEquals(android.bluetooth.BluetoothAdapter.STATE_OFF, RecorderBluetoothPolicy.STATE_OFF)
        assertEquals(android.bluetooth.BluetoothAdapter.STATE_TURNING_ON, RecorderBluetoothPolicy.STATE_TURNING_ON)
        assertEquals(android.bluetooth.BluetoothAdapter.STATE_ON, RecorderBluetoothPolicy.STATE_ON)
        assertEquals(android.bluetooth.BluetoothAdapter.STATE_TURNING_OFF, RecorderBluetoothPolicy.STATE_TURNING_OFF)
    }
}
