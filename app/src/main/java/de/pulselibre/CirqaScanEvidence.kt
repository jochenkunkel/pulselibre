package de.pulselibre

/** Scan evidence is intentionally conservative: hints never prove CIRQA identity. */
class CirqaScanEvidence {
    private val confirmed = mutableSetOf<String>()
    private val hints = mutableSetOf<String>()
    private val others = mutableSetOf<String>()

    fun reset() {
        confirmed.clear()
        hints.clear()
        others.clear()
    }

    fun observe(address: String, addressMatchesBond: Boolean, garminHint: Boolean) {
        confirmed.remove(address)
        hints.remove(address)
        others.remove(address)
        when {
            addressMatchesBond -> confirmed.add(address)
            garminHint -> hints.add(address)
            else -> others.add(address)
        }
    }

    fun summary(): String = buildString {
        appendLine("CIRQA-Sichtbarkeit im letzten Scan")
        appendLine("Bestätigte CIRQA-Werbeadressen: ${confirmed.size}")
        appendLine("Garmin-Hinweise (nicht CIRQA-bestätigt): ${hints.size}")
        appendLine("Nicht zuordenbare Werbegeräte: ${others.size}")
        if (confirmed.isEmpty()) append("CIRQA nicht eindeutig im Scan gesehen. Android-Kopplung und GATT-Status sind davon unabhängig.")
        else append("Mindestens eine Werbeadresse stimmt mit der Android-Kopplung überein.")
    }
}
