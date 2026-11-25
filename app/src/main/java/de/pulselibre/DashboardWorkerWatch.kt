package de.pulselibre

import android.content.Context
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

/**
 * The single background thread that serves the overview tiles and the day dialogs, and a diagnostic for it.
 *
 * A dialog that still says "reading" after [STALL_AFTER_MS] records where this thread is stuck (stack frames only, never
 * measured values) in private app storage. It appears in the user-exported diagnostics package (no logcat, A6-5).
 */
internal object DashboardWorkerWatch {
    const val THREAD_NAME = "pulselibre-dashboard"
    const val MAINTENANCE_THREAD_NAME = "pulselibre-maintenance"
    const val STALL_AFTER_MS = 15_000L
    private const val PREFS = "dashboard_diagnostics"
    private const val KEY_LAST_STALL = "last_stall"

    fun newDashboardWorker(): ExecutorService = Executors.newSingleThreadExecutor { r -> Thread(r, THREAD_NAME) }

    /** Long, best-effort work (FIT re-indexing) that must never make the tiles and dialogs wait. */
    fun newMaintenanceWorker(): ExecutorService = Executors.newSingleThreadExecutor { r ->
        Thread(r, MAINTENANCE_THREAD_NAME).apply { priority = Thread.MIN_PRIORITY }
    }

    /** Stack of every app worker thread, for [recordStall]; pure so it can be tested. */
    fun describe(threads: Map<Thread, Array<StackTraceElement>>): String = threads.entries
        .filter { it.key.name == THREAD_NAME || it.key.name == MAINTENANCE_THREAD_NAME }
        .sortedBy { it.key.name }
        .joinToString("\n") { (thread, stack) ->
            "${thread.name} ${thread.state}:\n" + stack.take(40).joinToString("\n") { "  at $it" }
        }

    fun recordStall(context: Context, where: String, nowMs: Long = System.currentTimeMillis()) {
        val report = "${java.time.Instant.ofEpochMilli(nowMs)} $where wartete > ${STALL_AFTER_MS / 1000} s\n" +
            describe(Thread.getAllStackTraces())
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putString(KEY_LAST_STALL, report.take(8_000)).apply()
    }

    fun lastStall(context: Context): String? =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(KEY_LAST_STALL, null)
}
