package de.pulselibre

/**
 * A structurally invalid 5037 after queuing the 5050 response is
 * unexpected synchronization traffic. Abort rather than infer progress.
 * Pure counter policy: no Bluetooth side effects.
 */
object Cirqa5050Malformed5037Guard {
    fun shouldAbort(baseline: Int, current: Int): Boolean =
        baseline < 0 || current < baseline || current > baseline
}
