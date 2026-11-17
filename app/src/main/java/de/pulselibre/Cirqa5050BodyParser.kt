package de.pulselibre

/**
 * Offline characterization of the Garmin Connect 5.31 5050 length-prefixed
 * capability bitmap. No transport or device interaction.
 */
object Cirqa5050BodyParser {
    data class Parsed(val declaredBitmapBytes: Int, val bitmap: ByteArray, val tailBytes: Int)
    fun parse(body: ByteArray): Parsed? {
        if (body.isEmpty()) return null
        val count = body[0].toInt() and 255
        if (body.size - 1 < count) return null
        return Parsed(count, body.copyOfRange(1, count + 1), body.size - count - 1)
    }
}
