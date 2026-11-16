package de.pulselibre

/**
 * Metadata-only inspection of Garmin Multi-Link notifications.
 * This is NOT a GFDI file-list parser: no payload is retained or logged.
 * Handle assignment is learned from a validated registration response.
 */
class GarminHistoryTransportInspector {
    var gfdiHandle: Int? = null
        private set
    var registrationReplies = 0
        private set
    var gfdiPackets = 0
        private set
    var otherPackets = 0
        private set
    var controlPackets = 0
        private set
    var gfdiPossibleMlrPackets = 0
        private set
    private val lengths = BoundedPacketLengths()

    fun observe(bytes: ByteArray) {
        val response = GarminMlOffline.inspectRegisterResponse(bytes)
        if (response?.serviceId == GarminMlHandshake.GFDI_SERVICE && response.accepted) {
            gfdiHandle = response.handle
            registrationReplies++
            return
        }
        lengths.record(bytes.size)
        val first = bytes.firstOrNull()?.toInt()?.and(255)
        when {
            first == 0 -> controlPackets++
            first != null && first == gfdiHandle -> {
                gfdiPackets++
                if (bytes.size >= 3) gfdiPossibleMlrPackets++
            }
            else -> otherPackets++
        }
    }

    fun summary(): String =
        "GFDI-Handle: ${gfdiHandle ?: "unbekannt"}; Registrierungen: $registrationReplies; " +
        "GFDI-Pakete: $gfdiPackets; Steuerpakete: $controlPackets; andere Pakete: $otherPackets; " +
        "GFDI mit mindestens 3 Bytes (MLR-Kandidat, unbestätigt): $gfdiPossibleMlrPackets; " +
        "Paketlängen: ${lengths.summary()}"
}
