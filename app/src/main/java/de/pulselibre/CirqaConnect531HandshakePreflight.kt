package de.pulselibre

/**
 * Fail-closed offline preflight for a future explicitly enabled 5024 experiment.
 * Does not enable BLE transmission or infer Garmin Connect's dynamic host values.
 */
object CirqaConnect531HandshakePreflight {
    enum class Blocker {
        DEVICE_5024_NOT_VERIFIED,
        FEATURE_FLAG_NOT_VERIFIED,
        APP_VERSION_NOT_VERIFIED,
        FRIENDLY_NAME_NOT_VERIFIED,
        MANUFACTURER_NOT_VERIFIED,
        MODEL_NOT_VERIFIED,
        EXPLICIT_TRIAL_AUTHORIZATION_MISSING
    }

    data class Evidence(
        val device5024Verified: Boolean = false,
        val featureFlagVerified: Boolean = false,
        val appVersionVerified: Boolean = false,
        val friendlyNameVerified: Boolean = false,
        val manufacturerVerified: Boolean = false,
        val modelVerified: Boolean = false,
        val explicitTrialAuthorized: Boolean = false
    )

    fun blockers(evidence: Evidence): Set<Blocker> = buildSet {
        if (!evidence.device5024Verified) add(Blocker.DEVICE_5024_NOT_VERIFIED)
        if (!evidence.featureFlagVerified) add(Blocker.FEATURE_FLAG_NOT_VERIFIED)
        if (!evidence.appVersionVerified) add(Blocker.APP_VERSION_NOT_VERIFIED)
        if (!evidence.friendlyNameVerified) add(Blocker.FRIENDLY_NAME_NOT_VERIFIED)
        if (!evidence.manufacturerVerified) add(Blocker.MANUFACTURER_NOT_VERIFIED)
        if (!evidence.modelVerified) add(Blocker.MODEL_NOT_VERIFIED)
        if (!evidence.explicitTrialAuthorized) add(Blocker.EXPLICIT_TRIAL_AUTHORIZATION_MISSING)
    }
}
