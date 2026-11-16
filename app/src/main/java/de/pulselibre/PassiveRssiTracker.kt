package de.pulselibre

/**
 * In-memory RSSI summaries only. Addresses are used as ephemeral map keys and are never exported.
 * RSSI is a noisy signal-strength measurement, not a distance or device-identity proof.
 */
class PassiveRssiTracker {
    private data class Samples(val values: MutableList<Int> = mutableListOf())
    private val samples = mutableMapOf<String, Samples>()
    private val candidateUuid = "3e1d50cd-7e3e-427d-8e1c-b78aa87fe624"

    fun reset() = samples.clear()

    fun observe(address: String, rssi: Int, serviceUuids: Collection<String>) {
        if (rssi !in -127..0) return
        // A UUID match is only a research candidate; do not label it as CIRQA.
        if (serviceUuids.none { it.equals(candidateUuid, ignoreCase = true) }) return
        val values = samples.getOrPut(address) { Samples() }.values
        // Bound memory for a long-running or noisy scan.
        if (values.size < 600) values.add(rssi)
    }

    fun summary(): String = buildString {
        appendLine("Passiver RSSI-Verlauf (UUID-Kandidat; keine CIRQA-Bestätigung)")
        appendLine("UUID: $candidateUuid")
        appendLine("Werbeadressen mit dieser UUID: ${samples.size}")
        val values = samples.values.flatMap { it.values }
        appendLine("RSSI-Messpunkte: ${values.size}")
        if (values.isNotEmpty()) {
            appendLine("RSSI min/max: ${values.minOrNull()} / ${values.maxOrNull()} dBm")
            appendLine("RSSI-Median: ${values.sorted()[values.size / 2]} dBm")
            appendLine("RSSI stark (>= -60 dBm): ${values.count { it >= -60 }}")
            appendLine("RSSI mittel (-75 bis -61 dBm): ${values.count { it in -75..-61 }}")
            appendLine("RSSI schwach (< -75 dBm): ${values.count { it < -75 }}")
            // 10-sample medians smooth short-term fluctuations; no timestamps or raw IDs exported.
            val windows = values.chunked(10).map { window -> window.sorted()[window.size / 2] }
            appendLine("RSSI-Verlauf (Median je 10 Messungen, dBm): ${windows.joinToString(", ")}")
        }
        append("RSSI ist kein Entfernungs- oder Identitätsbeweis.")
    }
}
