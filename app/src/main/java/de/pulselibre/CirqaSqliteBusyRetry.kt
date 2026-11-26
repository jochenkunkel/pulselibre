package de.pulselibre

import android.database.sqlite.SQLiteDatabaseLockedException
import android.database.sqlite.SQLiteTableLockedException

/** Small bounded retry for SQLite BUSY/LOCKED during database open/short archive transactions. */
internal object CirqaSqliteBusyRetry {
    private val delaysMillis = longArrayOf(50, 100, 250, 500)

    fun <T> run(
        pause: (Long) -> Unit = { Thread.sleep(it) },
        retryable: (Exception) -> Boolean = {
            it is SQLiteDatabaseLockedException || it is SQLiteTableLockedException
        },
        operation: () -> T
    ): T = runWithDelays(delaysMillis, pause, retryable, operation)

    /**
     * One retry for the sync DB open + idempotent import boundary. A native
     * SQLiteOpenHelper busy wait may already take several seconds.
     */
    fun <T> runOnce(
        pause: (Long) -> Unit = { Thread.sleep(it) },
        retryable: (Exception) -> Boolean = {
            it is SQLiteDatabaseLockedException || it is SQLiteTableLockedException
        },
        operation: () -> T
    ): T = runWithDelays(longArrayOf(250), pause, retryable, operation)

    private fun <T> runWithDelays(
        delays: LongArray,
        pause: (Long) -> Unit,
        retryable: (Exception) -> Boolean,
        operation: () -> T
    ): T {
        var attempt = 0
        while (true) {
            try {
                return operation()
            } catch (failure: Exception) {
                if (!retryable(failure)) throw failure
                if (attempt >= delays.size) throw failure
                val delay = delays[attempt++]
                try {
                    pause(delay)
                } catch (interrupted: InterruptedException) {
                    Thread.currentThread().interrupt()
                    throw failure
                }
            }
        }
    }
}
