package de.pulselibre

/** Offline-only structural analysis of a published fenix-8 RESPONSE(5000) to 5024.
 * Does not retain, transmit or emulate the captured watch identity.
 */
object GarminDeviceInformationReference {
    data class Structure(
        val frameBytes: Int,
        val extensionBytes: Int,
        val leadingBinaryBytes: Int,
        val firstTextBytes: Int,
        val secondTextBytes: Int,
        val thirdTextBytes: Int,
        val trailingBinaryBytes: Int
    )

    /** Inspect layout without interpreting unknown binary fields as CIRQA capabilities. */
    fun inspect(frame: ByteArray): Structure? {
        if (GarminFrameValidator.inspectVerified(frame) == null || frame.size < 9) return null
        fun u16(at: Int) = (frame[at].toInt() and 255) or ((frame[at + 1].toInt() and 255) shl 8)
        if (u16(0) != frame.size || u16(2) != 5000 || u16(4) != 5024 || frame[6] != 0.toByte()) return null
        val ext = frame.copyOfRange(7, frame.size - 2)
        // Observed capture: 12 opaque bytes, then two length-prefixed ASCII labels,
        // then one opaque byte. This is a layout observation, NOT a protocol schema.
        if (ext.size < 15) return null
        val firstLength = ext[12].toInt() and 255
        val secondLengthAt = 13 + firstLength
        if (firstLength == 0 || secondLengthAt >= ext.size) return null
        val secondLength = ext[secondLengthAt].toInt() and 255
        val thirdLengthAt = secondLengthAt + 1 + secondLength
        if (secondLength == 0 || thirdLengthAt >= ext.size) return null
        val thirdLength = ext[thirdLengthAt].toInt() and 255
        val end = thirdLengthAt + 1 + thirdLength
        if (thirdLength == 0 || end > ext.size) return null
        val first = ext.copyOfRange(13, secondLengthAt)
        val second = ext.copyOfRange(secondLengthAt + 1, thirdLengthAt)
        val third = ext.copyOfRange(thirdLengthAt + 1, end)
        if (!first.all { (it.toInt() and 255) in 32..126 } ||
            !second.all { (it.toInt() and 255) in 32..126 } ||
            !third.all { (it.toInt() and 255) in 32..126 }) return null
        return Structure(frame.size, ext.size, 12, firstLength, secondLength, thirdLength, ext.size - end)
    }
}
