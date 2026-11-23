package de.pulselibre

/**
 * Mutual exclusion of the two historical-download triggers (FIT request 23.11.): a manual start must never close or
 * replace a running automatic run, and the automatic resume trigger must never start while a manual run is active.
 * A manual run is not blocked by a previous manual run (the existing "restart" behaviour stays).
 */
internal object HistorySyncExclusion {
    fun canStartManual(automaticActive: Boolean): Boolean = !automaticActive
    fun canStartAutomatic(manualActive: Boolean): Boolean = !manualActive
}
