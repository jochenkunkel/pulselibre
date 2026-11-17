package de.pulselibre

import android.content.Context
import android.os.Build

/**
 * Read-only Android integration for the reconstructed Connect 5.31 host reply.
 *
 * Build.MANUFACTURER and Build.MODEL can be observed directly. The Garmin
 * hostConfig appVersion, friendlyName and feature flag are NOT equivalent to
 * the PulsLibre package version or Android Bluetooth device name.
 *
 * Never returns a sendable 5024 response; does not use Bluetooth.
 */
object Cirqa5024AndroidHostEvidence {
    data class Snapshot(
        val manufacturerAvailable: Boolean,
        val modelAvailable: Boolean,
        val appVersionResolved: Boolean = false,
        val friendlyNameResolved: Boolean = false,
        val featureFlagResolved: Boolean = false,
        val deviceProtocolResolved: Boolean = false
    ) {
        fun summary(): String =
            "5024-Hostdaten (nur lokal): Hersteller vorhanden=$manufacturerAvailable; " +
                "Modell vorhanden=$modelAvailable; Garmin-Appversion=UNGEKLÄRT; " +
                "FriendlyName=UNGEKLÄRT; FeatureFlag=UNGEKLÄRT; " +
                "Geräteprotokoll=UNGEKLÄRT; Senden=GESPERRT"
    }

    @Suppress("UNUSED_PARAMETER")
    fun inspect(context: Context): Snapshot = Snapshot(
        manufacturerAvailable = !Build.MANUFACTURER.isNullOrBlank(),
        modelAvailable = !Build.MODEL.isNullOrBlank()
    )

    /** An Android Build value is not proof of Garmin's hostConfig semantics. */
    fun preflight(snapshot: Snapshot): Set<CirqaConnect531HandshakePreflight.Blocker> =
        CirqaConnect531HandshakePreflight.blockers(
            CirqaConnect531HandshakePreflight.Evidence(
                manufacturerVerified = snapshot.manufacturerAvailable,
                modelVerified = snapshot.modelAvailable
            )
        )
}
