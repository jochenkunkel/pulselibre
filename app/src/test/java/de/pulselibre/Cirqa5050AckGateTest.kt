package de.pulselibre

import org.junit.Assert.*
import org.junit.Test

class Cirqa5050AckGateTest {
    @Test fun requiresAllThreeIndependentConditions() {
        for (queued in listOf(false, true)) {
            for (written in listOf(false, true)) {
                for (ack in listOf(false, true)) {
                    assertEquals(queued && written && ack,
                        Cirqa5050AckGate.isConfirmed(queued, written, ack))
                }
            }
        }
    }

    @Test fun prematureAckDoesNotCount() {
        assertFalse(Cirqa5050AckGate.isConfirmed(true, false, true))
        assertTrue(Cirqa5050AckGate.isConfirmed(true, true, true))
    }
}
