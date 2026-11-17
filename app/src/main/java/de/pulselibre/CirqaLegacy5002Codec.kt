package de.pulselibre

/**
 * Offline-only Garmin Connect 5.31 legacy GFDI 5002 body codec.
 * Evidence: lbb.T=u16LE, lbb.U=u32LE, a60.F=unsigned u32LE.
 * Not proof of CIRQA support. No transport, BLE or send operation.
 */
object CirqaLegacy5002Codec {
    fun request(fileIndex: Int, compressed: Boolean): ByteArray {
        require(fileIndex in 0..0xffff)
        return byteArrayOf(
            fileIndex.toByte(), (fileIndex ushr 8).toByte(),
            0, 0, 0, 0, 1, 0, 0, 0, 0, 0, 0,
            if (compressed) 1 else 0
        )
    }

    /** Null for malformed/unknown status; unsigned value only for SUCCESS. */
    data class Response(
        val status: CirqaLegacyTransferDecoder.ReadStatus,
        val unsignedValue: Long?
    )

    fun response(body: ByteArray): Response? {
        val decoded = CirqaLegacyTransferDecoder.readResponse(body) ?: return null
        val bytes = decoded.sizeBytes ?: return Response(decoded.status, null)
        val value = (bytes[0].toLong() and 255) or
            ((bytes[1].toLong() and 255) shl 8) or
            ((bytes[2].toLong() and 255) shl 16) or
            ((bytes[3].toLong() and 255) shl 24)
        return Response(decoded.status, value)
    }

    fun boundedSuccessSize(body: ByteArray, maxBytes: Int): Int? {
        require(maxBytes > 0)
        val parsed = response(body) ?: return null
        if (parsed.status != CirqaLegacyTransferDecoder.ReadStatus.SUCCESS) return null
        val value = parsed.unsignedValue ?: return null
        return value.takeIf { it in 1..maxBytes.toLong() }?.toInt()
    }
}
