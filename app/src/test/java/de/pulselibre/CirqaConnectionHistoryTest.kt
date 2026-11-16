package de.pulselibre

import org.junit.Assert.*
import org.junit.Test

class CirqaConnectionHistoryTest {
    @Test fun ignoresRepeatedStates() {
        val history = CirqaConnectionHistory()
        assertTrue(history.observe(100, CirqaConnectionHistory.State.CONNECTED))
        assertFalse(history.observe(200, CirqaConnectionHistory.State.CONNECTED))
        assertEquals(listOf(CirqaConnectionHistory.Entry(100, CirqaConnectionHistory.State.CONNECTED)), history.snapshot())
    }

    @Test fun preservesTransitionsAndBoundsMemory() {
        val history = CirqaConnectionHistory(2)
        history.observe(1, CirqaConnectionHistory.State.CONNECTED)
        history.observe(2, CirqaConnectionHistory.State.DISCONNECTED)
        history.observe(3, CirqaConnectionHistory.State.CONNECTED)
        assertEquals(listOf(2L, 3L), history.snapshot().map { it.timestampMillis })
    }

    @Test fun unknownAndPermissionDeniedAreDifferent() {
        val history = CirqaConnectionHistory()
        history.observe(1, CirqaConnectionHistory.State.UNKNOWN)
        history.observe(2, CirqaConnectionHistory.State.PERMISSION_DENIED)
        assertEquals(2, history.snapshot().size)
    }

    @Test(expected = IllegalArgumentException::class)
    fun rejectsInvalidCapacity() { CirqaConnectionHistory(0) }

    @Test(expected = IllegalArgumentException::class)
    fun rejectsNegativeTimestamp() { CirqaConnectionHistory().observe(-1, CirqaConnectionHistory.State.UNKNOWN) }
}
