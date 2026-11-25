package de.pulselibre

import org.junit.Assert.assertEquals
import org.junit.Test

class CirqaSqliteBusyRetryTest {
    private class BusyException : RuntimeException("synthetic SQLITE_BUSY")

    @Test fun retriesOnlyTheSpecificDatabaseLockedExceptionWithBoundedBackoff() {
        var attempts = 0
        val pauses = mutableListOf<Long>()
        val result = CirqaSqliteBusyRetry.run(
            pause = pauses::add,
            retryable = { it is BusyException }
        ) {
            attempts++
            if (attempts < 3) throw BusyException()
            "archived"
        }
        assertEquals("archived", result)
        assertEquals(3, attempts)
        assertEquals(listOf(50L, 100L), pauses)
    }

    @Test fun doesNotRetryUnrelatedFailures() {
        var attempts = 0
        try {
            CirqaSqliteBusyRetry.run(pause = {}, retryable = { it is IllegalArgumentException }) {
                attempts++
                throw BusyException()
            }
            throw AssertionError("expected synthetic failure")
        } catch (_: BusyException) {
            assertEquals(1, attempts)
        }
    }
}
