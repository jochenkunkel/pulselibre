package de.pulselibre

/**
 * Passive, offline-only structural interpretation of a CRC-verified GFDI 5050.
 * Public Garmin implementations encode capability bytes with a one-byte length.
 * The CIRQA payload semantics are not independently verified.
 */
object Cirqa5050OfflineCapabilities {
    data class Shape(val bodyLength: Int, val declaredLength: Int, val actualLength: Int,
                     val lengthMatches: Boolean, val setBits: Int)

    fun inspect(frame: ByteArray): Shape? {
        if (GarminFrameValidator.inspectVerified(frame) == null || frame.size < 7) return null
        fun u16(at: Int) = (frame[at].toInt() and 255) or ((frame[at + 1].toInt() and 255) shl 8)
        if (u16(2) != 5050) return null
        val bodyLength = frame.size - 6
        val declared = frame[4].toInt() and 255
        val actual = bodyLength - 1
        var setBits = 0
        for (i in 5 until frame.size - 2) {
            setBits += Integer.bitCount(frame[i].toInt() and 255)
        }
        return Shape(bodyLength, declared, actual, declared == actual, setBits)
    }
}
