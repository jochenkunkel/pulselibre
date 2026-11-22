package de.pulselibre

import java.io.ByteArrayOutputStream
import java.util.zip.DataFormatException
import java.util.zip.Inflater

/**
 * Offline decoder for a complete, already reassembled zlib file payload.
 * Does not strip ML headers, negotiate compression, send BLE or import data.
 * Callers must still verify file format/CRC before storing or importing output.
 */
object CirqaZlibFileDecoder {
    const val MAX_BYTES = 16 * 1024 * 1024

    fun decode(compressed: ByteArray, expectedSize: Int): ByteArray? {
        if (expectedSize !in 1..MAX_BYTES || compressed.isEmpty() ||
            compressed.size > MAX_BYTES) return null
        val inflater = Inflater()
        return try {
            inflater.setInput(compressed)
            val output = ByteArrayOutputStream(minOf(expectedSize, 8192))
            val buffer = ByteArray(8192)
            while (!inflater.finished()) {
                val count = inflater.inflate(buffer)
                if (count > expectedSize - output.size()) return null
                if (count > 0) output.write(buffer, 0, count)
                else if (!inflater.finished()) return null // dictionary, truncation or no progress
            }
            if (inflater.remaining != 0 || output.size() != expectedSize) null
            else output.toByteArray()
        } catch (_: DataFormatException) {
            null
        } finally {
            inflater.end()
        }
    }
}
