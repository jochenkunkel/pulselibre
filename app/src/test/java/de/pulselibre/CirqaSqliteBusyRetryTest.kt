package de.pulselibre

import android.database.sqlite.SQLiteDatabaseLockedException
import android.database.sqlite.SQLiteTableLockedException
import org.junit.Assert.assertEquals
import org.junit.Test

class CirqaSqliteBusyRetryTest {
    private class BusyException : RuntimeException("synthetic SQLITE_BUSY")

    @Test fun defaultPredicateRetriesActualSqliteBusyDatabaseLockException() {
        var attempts = 0
        val pauses = mutableListOf<Long>()
        val result = CirqaSqliteBusyRetry.run(pause = pauses::add) {
            attempts++
            if (attempts == 1) throw SQLiteDatabaseLockedException("database busy (SQLITE_BUSY)")
            "archived"
        }

        assertEquals("archived", result)
        assertEquals(2, attempts)
        assertEquals(listOf(50L), pauses)
    }

    @Test fun defaultPredicateRetriesActualSqliteLockedTableException() {
        var attempts = 0
        val pauses = mutableListOf<Long>()
        val result = CirqaSqliteBusyRetry.runOnce(pause = pauses::add) {
            attempts++
            if (attempts == 1) throw SQLiteTableLockedException("table locked (SQLITE_LOCKED)")
            "opened"
        }

        assertEquals("opened", result)
        assertEquals(2, attempts)
        assertEquals(listOf(250L), pauses)
    }

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

    @Test fun openRetryUsesOneShortDelayAfterNativeBusyWait() {
        var attempts = 0
        val pauses = mutableListOf<Long>()
        val result = CirqaSqliteBusyRetry.runOnce(
            pause = pauses::add,
            retryable = { it is BusyException }
        ) {
            attempts++
            if (attempts == 1) throw BusyException()
            "opened"
        }

        assertEquals("opened", result)
        assertEquals(2, attempts)
        assertEquals(listOf(250L), pauses)
    }
}
