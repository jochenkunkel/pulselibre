package de.pulselibre

/** Scan evidence is intentionally conservative: hints never prove CIRQA identity. */
class CirqaScanEvidence {
    private val confirmed = mutableSetOf<String>()
    private val hints = mutableSetOf<String>()
    private val others = mutableSetOf<String>()
    private val observations = mutableMapOf<String, AdvertisementFeatures>()
    private var totalObservations = 0

    data class AdvertisementFeatures(
        val hasName: Boolean,
        val hasManufacturerData: Boolean,
        val hasServiceUuids: Boolean
    )

    fun reset() {
        confirmed.clear()
        hints.clear()
        others.clear()
        observations.clear()
        totalObservations = 0
    }

    fun observe(address: String, addressMatchesBond: Boolean, garminHint: Boolean) {
        // A later advertisement can omit fields; retain strongest prior evidence.
        when {
            addressMatchesBond -> {
                hints.remove(address)
                others.remove(address)
                confirmed.add(address)
            }
            address in confirmed -> Unit
            garminHint -> {
                others.remove(address)
                hints.add(address)
            }
            address !in hints -> others.add(address)
        }
    }

    /** Aggregate only non-identifying presence flags; never export addresses or payloads. */
    fun observeFeatures(address: String, hasName: Boolean, hasManufacturerData: Boolean, hasServiceUuids: Boolean) {
        totalObservations++
        val previous = observations[address]
        observations[address] = AdvertisementFeatures(
            hasName || previous?.hasName == true,
            hasManufacturerData || previous?.hasManufacturerData == true,
            hasServiceUuids || previous?.hasServiceUuids == true
        )
    }

    fun summary(): String = buildString {
        appendLine("CIRQA-Sichtbarkeit im letzten Scan")
        appendLine("Adressgleichheit mit CIRQA-Kopplung: ${confirmed.size}")
        appendLine("Garmin-Hinweise (nicht CIRQA-bestätigt): ${hints.size}")
        appendLine("Nicht zuordenbare Werbegeräte: ${others.size}")
        appendLine("Werbeereignisse insgesamt: $totalObservations (mehrfach pro Gerät möglich)")
        appendLine("Geräte mit sichtbarem Namen: ${observations.values.count { it.hasName }}")
        appendLine("Geräte mit Herstellerdaten: ${observations.values.count { it.hasManufacturerData }}")
        appendLine("Geräte mit Service-UUIDs: ${observations.values.count { it.hasServiceUuids }}")
        if (confirmed.isEmpty()) append("CIRQA nicht eindeutig im Scan identifiziert. Android-Kopplung und GATT-Status sind davon unabhängig.")
        else append("Mindestens eine Werbeadresse stimmt mit der Android-Kopplung überein; kein Identitätsbeweis.")
    }
}
