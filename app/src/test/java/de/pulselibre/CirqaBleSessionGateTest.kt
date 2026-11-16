package de.pulselibre

import org.junit.Assert.*
import org.junit.Test

class CirqaBleSessionGateTest {
    @Test fun preventsConcurrentOwnersAndAllowsNextAfterRelease() {
        val first = Any()
        val second = Any()
        assertTrue(CirqaBleSessionGate.acquire(first))
        assertTrue(CirqaBleSessionGate.isBusy())
        assertFalse(CirqaBleSessionGate.acquire(second))
        CirqaBleSessionGate.release(second)
        assertTrue(CirqaBleSessionGate.isBusy())
        CirqaBleSessionGate.release(first)
        assertFalse(CirqaBleSessionGate.isBusy())
        assertTrue(CirqaBleSessionGate.acquire(second))
        CirqaBleSessionGate.release(second)
    }
}
