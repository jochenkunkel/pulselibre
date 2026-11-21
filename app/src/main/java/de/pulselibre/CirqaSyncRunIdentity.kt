package de.pulselibre

/**
 * Redacted, stable identifier for distinguishing a completed sync report from
 * a previous report that happens to have the same counters and outcome.
 */
internal object CirqaSyncRunIdentity {
    private val uuidPattern =
        Regex("^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$")

    fun reportField(runId: String?): String {
        val token = runId
            ?.takeIf(uuidPattern::matches)
            ?.replace("-", "")
            ?.take(12)
            ?.lowercase()
            ?: "unavailable"
        return "runId=$token"
    }
}
