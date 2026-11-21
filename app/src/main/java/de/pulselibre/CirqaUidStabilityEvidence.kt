package de.pulselibre

/**
 * Local evidence that one remote-item identity repeated in distinct,
 * successful sessions. This is observational evidence, not a guarantee about
 * future firmware behavior or the contents of a remote file.
 */
class CirqaUidStabilityEvidence private constructor(
    val matchingSuccessfulSessions: Int,
    val identityFingerprint: String
) {
    data class SessionObservation(
        val sessionId: String,
        val accepted: Boolean,
        val itemListReceived: Boolean,
        val identityFingerprint: String
    )

    companion object {
        const val MIN_MATCHING_SESSIONS = 3

        /**
         * Returns evidence only when at least three distinct sessions each
         * accepted sync, received a list, and observed the same valid digest.
         * Failed attempts cannot count as evidence for or against stability.
         */
        fun from(observations: List<SessionObservation>): CirqaUidStabilityEvidence? {
            if (observations.size < MIN_MATCHING_SESSIONS) return null
            if (observations.any {
                    it.sessionId.isBlank() ||
                        !it.accepted ||
                        !it.itemListReceived ||
                        !it.identityFingerprint.matches(Regex("[0-9a-f]{64}"))
                }
            ) return null
            if (observations.map { it.sessionId }.toSet().size != observations.size) return null

            val fingerprints = observations.map { it.identityFingerprint }.toSet()
            if (fingerprints.size != 1) return null

            return CirqaUidStabilityEvidence(observations.size, fingerprints.single())
        }
    }
}
