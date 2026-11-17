package de.pulselibre

/** Bounded, passive metadata-only inspection of CRC-verified GFDI type 5050.
 * No payload bytes, identifiers, or unknown ACKs are emitted.
 */
class Cirqa5050StructureObserver {
    private var count = 0
    private var duplicates = 0
    private var changes = 0
    private var previous: ByteArray? = null
    private val bodyLengths = linkedMapOf<Int, Int>()
    private val prefixLengths = linkedMapOf<Int, Int>()
    private val firstBodyByteFrequencies = linkedMapOf<Int, Int>()

    fun observe(frame: ByteArray) {
        if (GarminFrameValidator.inspectVerified(frame) == null || frame.size < 6) return
        fun u16(at: Int) = (frame[at].toInt() and 255) or ((frame[at + 1].toInt() and 255) shl 8)
        if (u16(2) != 5050) return
        val body = frame.copyOfRange(4, frame.size - 2)
        if (count < Int.MAX_VALUE) count++
        bodyLengths[body.size] = (bodyLengths[body.size] ?: 0).let { if (it < Int.MAX_VALUE) it + 1 else it }
        previous?.let { old ->
            if (old.contentEquals(body)) {
                if (duplicates < Int.MAX_VALUE) duplicates++
            } else {
                if (changes < Int.MAX_VALUE) changes++
                var same = 0
                while (same < old.size && same < body.size && old[same] == body[same]) same++
                prefixLengths[same] = (prefixLengths[same] ?: 0) + 1
            }
        }
        // Only number of distinct first-byte values, never the values themselves.
        if (body.isNotEmpty()) firstBodyByteFrequencies[body[0].toInt() and 255] = 1
        previous = body
    }

    fun summary(): String = "5050-Struktur (passiv): Frames=$count; identische Wiederholungen=$duplicates; " +
        "Payloadwechsel=$changes; Body-Längen=$bodyLengths; " +
        "gemeinsame Präfixlängen bei Wechsel=$prefixLengths; " +
        "unterschiedliche erste Body-Bytes=${firstBodyByteFrequencies.size}; " +
        "5050-Format=UNBEKANNT; automatische Antwort=NEIN"
}
