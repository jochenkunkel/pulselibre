package de.pulselibre

import androidx.appcompat.app.AlertDialog

/**
 * Remembers the most recently shown settings/profile dialog so instrumentation tests can inspect and click it
 * directly. UiAutomation's "active window" proved unreliable on freshly booted CI emulators; this is deterministic.
 * Holds only a weak reference: a dismissed dialog (and with it its activity) must not stay in memory until the next
 * dialog appears. A showing dialog is held by the window manager anyway. Never used by production logic.
 */
internal object DialogProbe {
    @Volatile private var ref: java.lang.ref.WeakReference<AlertDialog>? = null
    val current: AlertDialog? get() = ref?.get()
    fun <T : AlertDialog> track(dialog: T): T { ref = java.lang.ref.WeakReference(dialog); return dialog }
}
