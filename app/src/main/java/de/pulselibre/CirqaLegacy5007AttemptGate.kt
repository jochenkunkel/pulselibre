package de.pulselibre

/**
 * Hard lifetime cap for the specifically authorized three-run 5007 review.
 * The legacy boolean is migrated as one consumed attempt; it can never clear
 * or lower a persisted count.
 */
object CirqaLegacy5007AttemptGate {
    const val MAX_AUTHORIZED_ATTEMPTS = 3

    fun consumedAttempts(storedCount: Int, legacyAttempted: Boolean): Int =
        maxOf(storedCount.coerceAtLeast(0), if (legacyAttempted) 1 else 0)
            .coerceAtMost(MAX_AUTHORIZED_ATTEMPTS)

    fun canAttempt(storedCount: Int, legacyAttempted: Boolean): Boolean =
        consumedAttempts(storedCount, legacyAttempted) < MAX_AUTHORIZED_ATTEMPTS

    fun nextAttempt(storedCount: Int, legacyAttempted: Boolean): Int? =
        consumedAttempts(storedCount, legacyAttempted)
            .takeIf { it < MAX_AUTHORIZED_ATTEMPTS }
            ?.plus(1)
}
