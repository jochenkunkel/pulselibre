package de.pulselibre

/** A single separately authorized 5031 probe; the 5007 lifetime gate is untouched. */
object CirqaLegacy5031AttemptGate {
    fun canAttempt(consumed: Boolean): Boolean = !consumed
}
