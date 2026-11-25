package de.pulselibre

import android.util.Log
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

/**
 * The single background thread that serves the overview tiles and the day dialogs, and a diagnostic for it.
 *
 * A dialog that still says "reading" after [STALL_AFTER_MS] logs where this thread is stuck (stack frames only, never
 * measured values), so a hang on a release build can be traced from logcat without a debugger.
 */
internal object DashboardWorkerWatch {
    const val THREAD_NAME = "pulselibre-dashboard"
    const val MAINTENANCE_THREAD_NAME = "pulselibre-maintenance"
    const val STALL_AFTER_MS = 15_000L
    private const val TAG = "PulsLibre"

    fun newDashboardWorker(): ExecutorService = Executors.newSingleThreadExecutor { r -> Thread(r, THREAD_NAME) }

    /** Long, best-effort work (FIT re-indexing) that must never make the tiles and dialogs wait. */
    fun newMaintenanceWorker(): ExecutorService = Executors.newSingleThreadExecutor { r ->
        Thread(r, MAINTENANCE_THREAD_NAME).apply { priority = Thread.MIN_PRIORITY }
    }

    /** Stack of every app worker thread, for [logStall]; pure so it can be tested. */
    fun describe(threads: Map<Thread, Array<StackTraceElement>>): String = threads.entries
        .filter { it.key.name == THREAD_NAME || it.key.name == MAINTENANCE_THREAD_NAME }
        .sortedBy { it.key.name }
        .joinToString("\n") { (thread, stack) ->
            "${thread.name} ${thread.state}:\n" + stack.take(40).joinToString("\n") { "  at $it" }
        }

    fun logStall(where: String) {
        Log.w(TAG, "$where still waiting after ${STALL_AFTER_MS / 1000} s\n" + describe(Thread.getAllStackTraces()))
    }
}
