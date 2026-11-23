package de.pulselibre

/**
 * One-shot, exception-safe terminal report boundary used by production finish().
 * Repeated terminal callbacks return the first immutable report.
 */
internal class CirqaSyncTerminalFinalizer {
    private var finalized: String? = null

    @Synchronized
    fun finish(build: () -> String, fallback: () -> String): String {
        finalized?.let { return it }
        val report = runCatching(build).getOrElse {
            runCatching(fallback).getOrDefault(
                "Sync-Beobachtung: terminalCause=diagnostic-failed; runDrained=false; " +
                    "incompleteReason=diagnostic-failed"
            )
        }
        finalized = report
        return report
    }

    @Synchronized
    fun reset() {
        finalized = null
    }
}
