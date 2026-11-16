package de.pulselibre

/**
 * Structural GFDI header interpretation based on the public Gadgetbridge Garmin
 * protocol documentation. Never constructs or transmits acknowledgements.
 */
object GarminGfdiHeader {
    data class Header(
        val type: Int,
        val sequence: Int?,
        val compact: Boolean,
        val responseTo: Int?,
        val responseStatus: Int?
    )

    fun inspect(frame: ByteArray): Header? {
        if (GarminFrameValidator.inspectVerified(frame) == null || frame.size < 6) return null
        val low = frame[2].toInt() and 0xff
        val high = frame[3].toInt() and 0xff
        val compact = (high and 0x80) != 0
        val type = if (compact) 5000 + low else low or (high shl 8)
        val sequence = if (compact) high and 0x7f else null
        val responseTo = if (type == 5000 && frame.size >= 9)
            (frame[4].toInt() and 0xff) or ((frame[5].toInt() and 0xff) shl 8)
        else null
        val responseStatus = if (responseTo != null) frame[6].toInt() and 0xff else null
        return Header(type, sequence, compact, responseTo, responseStatus)
    }

    fun describe(frame: ByteArray): String {
        val h = inspect(frame) ?: return "GFDI-Header=UNGÜLTIG"
        return "GFDI-Header: Typ=" + h.type +
            "; Format=" + (if (h.compact) "kompakt" else "16-bit") +
            "; Sequenz=" + (h.sequence?.toString() ?: "keine") +
            "; Antwort-auf=" + (h.responseTo?.toString() ?: "keine") +
            "; Status=" + (h.responseStatus?.toString() ?: "keiner")
    }
}
