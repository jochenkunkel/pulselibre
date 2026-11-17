package de.pulselibre

/**
 * Strict, offline-only inspection of the GFDI 5037 synchronization BODY.
 *
 * Reference: Amazfish GarminSynchronizationMessage::parse (2026-11-17).
 * This is a structural parser, NOT proof that CIRQA sends 5037, and it never
 * generates a response or interacts with BLE.
 */
object Cirqa5037OfflineSynchronization {
    data class Shape(
        val synchronizationType: Int,
        val maskByteCount: Int,
        val fileTypeMask: ULong,
        val setFileTypeBits: List<Int>
    )

    fun inspect(body: ByteArray): Shape? {
        if (body.size < 2) return null
        val size = body[1].toInt() and 0xff
        if (size != 4 && size != 8) return null
        if (body.size != size + 2) return null
        var mask = 0uL
        for (index in 0 until size) {
            mask = mask or ((body[index + 2].toULong() and 0xffuL) shl (index * 8))
        }
        val bits = (0 until size * 8).filter { (mask and (1uL shl it)) != 0uL }
        return Shape(
            synchronizationType = body[0].toInt() and 0xff,
            maskByteCount = size,
            fileTypeMask = mask,
            setFileTypeBits = bits
        )
    }
}
