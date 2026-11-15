package de.pulselibre

/**
 * Offline-only Garmin Multi-Link protocol inspection.
 * No Android Bluetooth imports, no writes, and no device identification claims.
 *
 * Based on documented ML control-channel response layouts, NOT on CIRQA captures.
 * Does not implement CLOSE_ALL, registration requests, or a live handshake.
 */
object GarminMlOffline {
    const val SERVICE_UUID = "6a4e2800-667b-11e3-949a-0800200c9a66"
    private const val BASE_SUFFIX = "-667b-11e3-949a-0800200c9a66"

    data class CharacteristicPair(val rx: String, val tx: String)

    /** Returns a complete known RX/TX pair, never an assumed or partial pair. */
    fun findCharacteristicPair(uuids: Collection<String>): CharacteristicPair? {
        val normalized = uuids.map { it.lowercase() }.toSet()
        for (rxId in 0x2810..0x2814) {
            val rx = "6a4e${rxId.toString(16).padStart(4, '0')}$BASE_SUFFIX"
            val tx = "6a4e${(rxId + 0x10).toString(16).padStart(4, '0')}$BASE_SUFFIX"
            if (rx in normalized && tx in normalized) return CharacteristicPair(rx, tx)
        }
        return null
    }

    data class RegisterResponse(val serviceId: Int, val status: Int, val handle: Int) {
        val accepted: Boolean get() = status == 0 && handle in 1..127
    }

    /**
     * Inspect a complete unencrypted ML control notification.
     * Layout: 0x00, response-type 0x01, 8-byte client ID, LE service ID,
     * status, handle. Only accepts a complete 14-byte response.
     */
    fun inspectRegisterResponse(packet: ByteArray): RegisterResponse? {
        if (packet.size != 14 || u8(packet[0]) != 0 || u8(packet[1]) != 1) return null
        val service = u8(packet[10]) or (u8(packet[11]) shl 8)
        return RegisterResponse(service, u8(packet[12]), u8(packet[13]))
    }

    /** A nonzero unflagged byte is a bare handle; flagged MLR uses bits 6:4. */
    fun inspectDataHandle(packet: ByteArray): Int? {
        if (packet.size < 2) return null
        val first = u8(packet[0])
        if (first == 0) return null // control channel, not data
        return if (first and 0x80 != 0) (first and 0x70) ushr 4 else first
    }

    private fun u8(value: Byte): Int = value.toInt() and 0xff
}
