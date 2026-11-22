package de.pulselibre

/**
 * Coverage marker for a completed observation run.
 *
 * A drained transfer (`runDrained`) only means that the bounded requests of
 * this run were processed. It is **not** evidence for a complete device
 * history: neither the FIT bytes as an independent channel nor the classic
 * transport side have been proven complete. Therefore this marker must never
 * be upgraded based on the local state of a single run.
 */
internal object CirqaSyncCoverage {
    const val DEVICE_HISTORY_UNVERIFIED = "device-history-unverified"

    // The parameters are intentionally ignored: no local run state may upgrade
    // the marker. Callers pass them so a regression test can pin every combination.
    fun marker(runDrained: Boolean, incompleteReason: String?): String =
        "coverage=$DEVICE_HISTORY_UNVERIFIED"
}
