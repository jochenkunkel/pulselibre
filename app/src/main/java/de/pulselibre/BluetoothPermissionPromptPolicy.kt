package de.pulselibre

/**
 * Pure decision for the initial permission request.
 * An explicit user action may request missing permissions again; this policy
 * controls only the unsolicited first-launch prompt.
 */
object BluetoothPermissionPromptPolicy {
    fun shouldPromptOnLaunch(initialPromptShown: Boolean, missingPermissionCount: Int): Boolean =
        !initialPromptShown && missingPermissionCount > 0
}
