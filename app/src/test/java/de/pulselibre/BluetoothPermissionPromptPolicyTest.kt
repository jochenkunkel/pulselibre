package de.pulselibre

import org.junit.Assert.*
import org.junit.Test

class BluetoothPermissionPromptPolicyTest {
    @Test fun firstLaunchWithMissingPermissionPrompts() {
        assertTrue(BluetoothPermissionPromptPolicy.shouldPromptOnLaunch(false, 2))
    }

    @Test fun grantedPermissionsNeverPrompt() {
        assertFalse(BluetoothPermissionPromptPolicy.shouldPromptOnLaunch(false, 0))
    }

    @Test fun previouslyPromptedDoesNotPromptAgainAutomatically() {
        assertFalse(BluetoothPermissionPromptPolicy.shouldPromptOnLaunch(true, 2))
    }

    @Test fun explicitRetryRemainsOutsideLaunchPolicy() {
        assertFalse(BluetoothPermissionPromptPolicy.shouldPromptOnLaunch(true, 1))
    }
}
