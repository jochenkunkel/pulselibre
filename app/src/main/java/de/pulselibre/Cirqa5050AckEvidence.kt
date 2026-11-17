package de.pulselibre

/**
 * Counts only validated GFDI-5000 ACKs for request 5050.
 * Snapshot at response queue time prevents pre-existing ACK evidence
 * from being misattributed to the current response.
 */
object Cirqa5050AckEvidence {
    fun count(statuses: Map<String, Int>): Int =
        statuses.entries.filter { (key, _) ->
            key.startsWith("request=5050/status=ACK/seq=")
        }.sumOf { (_, count) -> count.coerceAtLeast(0) }

    fun isNewAck(baseline: Int, statuses: Map<String, Int>): Boolean =
        baseline >= 0 && count(statuses) > baseline
}
