package de.pulselibre

import de.pulselibre.CirqaConnectionHistory.State

/**
 * UI status of the CIRQA link, derived from three independent facts:
 * recorder service flag, Android's GATT state, and the last reading the recorder actually received.
 * A running service alone is never "online"; fresh readings count as proof of an active link even
 * if Android's GATT query briefly lags or reports a stale state during a reconnect.
 */
object CirqaLinkStatus {
    /** Readings arrive about every few seconds; beyond this age a link is no longer evidenced by data. */
    const val FRESH_READING_MS = 120_000L

    enum class Kind { ONLINE_RECEIVING, ONLINE_WAITING, RECONNECTING, RECORDER_OFF_DISCONNECTED, UNAVAILABLE }

    data class Result(val kind: Kind, val online: Boolean, val label: String)

    fun evaluate(recorderRunning: Boolean, gatt: State, lastReadingMs: Long?, nowMs: Long): Result {
        if (gatt == State.NOT_PAIRED || gatt == State.PERMISSION_DENIED) {
            return Result(Kind.UNAVAILABLE, false, CirqaStateMapping.label(gatt))
        }
        val age = lastReadingMs?.takeIf { it in 0..nowMs }?.let { nowMs - it }
        val fresh = recorderRunning && age != null && age <= FRESH_READING_MS
        return when {
            fresh -> Result(Kind.ONLINE_RECEIVING, true, "Verbunden · letzte Messung vor ${ageText(age!!)}")
            gatt == State.CONNECTED -> Result(Kind.ONLINE_WAITING, true,
                if (recorderRunning) "Verbunden · warte auf Messwerte" else "GATT verbunden (Aufzeichnung aus)")
            recorderRunning -> Result(Kind.RECONNECTING, false,
                "Aufzeichnung läuft · Verbindung unterbrochen, Wiederverbindung" +
                    (if (age != null) " · letzte Messung vor ${ageText(age)}" else ""))
            else -> Result(Kind.RECORDER_OFF_DISCONNECTED, false, "Keine Verbindung · Aufzeichnung aus")
        }
    }

    fun ageText(ms: Long): String {
        val s = ms / 1000
        return when {
            s < 60 -> "${s} s"
            s < 3600 -> "${s / 60} min"
            else -> "${s / 3600} h ${(s % 3600) / 60} min"
        }
    }

    fun contentDescription(result: Result): String =
        "CIRQA-Verbindungsstatus: ${if (result.online) "grüner Rand" else "schwarzer Rand"}. ${result.label}. Einstellungen öffnen."
}
