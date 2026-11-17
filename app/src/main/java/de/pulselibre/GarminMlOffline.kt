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

    /**
     * Garmin Connect 5.31 scans ML data characteristics 0x2810..0x2819.
     * Its GATT transport uses the corresponding 0x2820..0x2829 characteristic
     * for writes when present, and falls back to the same characteristic when
     * that counterpart is absent. This is APK-derived behavior, not a CIRQA
     * capture or proof that every device exposes every candidate.
     */
    fun characteristicPairCandidates(uuids: Collection<String>): List<CharacteristicPair> {
        val normalized = uuids.map { it.lowercase() }.toSet()
        return (0x2810..0x2819).mapNotNull { rxId ->
            val rx = "6a4e${rxId.toString(16).padStart(4, '0')}$BASE_SUFFIX"
            if (rx !in normalized) return@mapNotNull null
            val tx = "6a4e${(rxId + 0x10).toString(16).padStart(4, '0')}$BASE_SUFFIX"
            CharacteristicPair(rx, if (tx in normalized) tx else rx)
        }
    }

    /** Returns the first Connect-compatible candidate, if any was discovered. */
    fun findCharacteristicPair(uuids: Collection<String>): CharacteristicPair? =
        characteristicPairCandidates(uuids).firstOrNull()

    data class RegisterResponse(val serviceId: Int, val status: Int, val handle: Int) {
        val accepted: Boolean get() = status == 0 && handle in 1..127
    }

    /**
     * Inspect a complete unencrypted ML control notification.
     * Layout: 0x00, response-type 0x01, 8-byte client ID, LE service ID,
     * status, handle. Parses the 14-byte core; later optional/extension bytes do not change its fixed offsets.\n     * An upper bound prevents treating arbitrarily large data as control frames.
     */
    fun inspectRegisterResponse(packet: ByteArray): RegisterResponse? {
        if (packet.size !in 14..64 || u8(packet[0]) != 0 || u8(packet[1]) != 1) return null
        // Multi-Link client ID is an unsigned LE 64-bit value (our client ID is 2).
        if (u8(packet[2]) != 2 || (3..9).any { u8(packet[it]) != 0 }) return null
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
