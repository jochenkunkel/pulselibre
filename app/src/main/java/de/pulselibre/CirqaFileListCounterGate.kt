package de.pulselibre

/**
 * Fail-closed decision for a single CIRQA FileList counter experiment.
 * No Bluetooth writes. Counter values must be proven from the *current*
 * session, never copied from another Garmin device or guessed.
 */
object CirqaFileListCounterGate {
    enum class Decision {
        ALLOW_ONE_SHOT, NOT_AUTHORIZED, NO_5050_ACK, NO_SESSION_EVIDENCE,
        WRONG_SESSION, INVALID_COUNTERS, ALREADY_SENT, NEGATIVE_RESPONSE
    }

    data class Evidence(
        val explicitlyAuthorized: Boolean,
        val configurationWriteConfirmed: Boolean,
        val fresh5050Ack: Boolean,
        val sessionId: String?,
        val counterEvidenceSessionId: String?,
        val sequence: Int?,
        val counter: Int?,
        val alreadySent: Boolean,
        val negativeResponseObserved: Boolean
    )

    fun decide(e: Evidence): Decision = when {
        !e.explicitlyAuthorized -> Decision.NOT_AUTHORIZED
        !e.configurationWriteConfirmed || !e.fresh5050Ack -> Decision.NO_5050_ACK
        e.negativeResponseObserved -> Decision.NEGATIVE_RESPONSE
        e.alreadySent -> Decision.ALREADY_SENT
        e.sessionId.isNullOrBlank() || e.counterEvidenceSessionId.isNullOrBlank() ||
            e.sequence == null || e.counter == null -> Decision.NO_SESSION_EVIDENCE
        e.sessionId != e.counterEvidenceSessionId -> Decision.WRONG_SESSION
        e.sequence !in 1..127 || e.counter !in 0..65535 -> Decision.INVALID_COUNTERS
        else -> Decision.ALLOW_ONE_SHOT
    }
}
