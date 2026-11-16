package de.pulselibre

/**
 * Process-local exclusive ownership of the CIRQA GATT transport.
 * A diagnostic never interrupts a recorder sample and vice versa.
 * Not a cross-process lock; all PulsLibre BLE probes run in the app process.
 */
object CirqaBleSessionGate {
    private var owner: Any? = null

    @Synchronized fun acquire(candidate: Any): Boolean {
        if (owner != null) return false
        owner = candidate
        return true
    }

    @Synchronized fun release(candidate: Any) {
        if (owner === candidate) owner = null
    }

    @Synchronized fun isBusy(): Boolean = owner != null
}
