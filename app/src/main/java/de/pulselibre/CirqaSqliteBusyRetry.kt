package de.pulselibre

import android.database.sqlite.SQLiteDatabaseLockedException

/** Small bounded retry for SQLITE_BUSY during database open/short archive transactions. */
internal object CirqaSqliteBusyRetry {
    private val delaysMillis = longArrayOf(100, 250, 500, 1_000)

    fun <T> run(
        pause: (Long) -> Unit = { Thread.sleep(it) },
        retryable: (Exception) -> Boolean = { it is SQLiteDatabaseLockedException },
        operation: () -> T
    ): T {
        var attempt = 0
        while (true) {
            try {
                return operation()
            } catch (failure: Exception) {
                if (!retryable(failure)) throw failure
                if (attempt >= delaysMillis.size) throw failure
                val delay = delaysMillis[attempt++]
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
