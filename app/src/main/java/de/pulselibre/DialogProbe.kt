package de.pulselibre

import androidx.appcompat.app.AlertDialog

/**
 * Remembers the most recently shown settings/profile dialog so instrumentation tests can inspect and click it
 * directly. UiAutomation's "active window" proved unreliable on freshly booted CI emulators; this is deterministic.
 * Holds a weak-ish reference only until the next dialog; never used by production logic.
 */
internal object DialogProbe {
    @Volatile var current: AlertDialog? = null
    fun <T : AlertDialog> track(dialog: T): T { current = dialog; return dialog }
}
