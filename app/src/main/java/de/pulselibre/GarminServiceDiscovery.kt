package de.pulselibre

/**
 * Read-only Garmin Multi-Link service discovery candidates.
 *
 * 16 is reported as accelerometer on some Garmin watches, NOT verified on CIRQA.
 * Other service numbers must not be blindly probed: device-specific semantics
 * may include stateful commands. Only explicitly reviewed candidates belong here.
 */
object GarminServiceDiscovery {
    val CANDIDATES: List<Int> = listOf(16)

    fun describe(
        accepted: Map<Int, Int>,
        rejected: List<Int>,
        frames: Map<Int, Int>
    ): String = CANDIDATES.joinToString("; ") { id ->
        when {
            id in accepted -> "Service $id registriert (Handle ${accepted[id]}, Pakete ${frames[id] ?: 0}); Inhalt nicht validiert"
            id in rejected -> "Service $id abgelehnt"
            else -> "Service $id ohne bestätigte Antwort"
        }
    }
}
