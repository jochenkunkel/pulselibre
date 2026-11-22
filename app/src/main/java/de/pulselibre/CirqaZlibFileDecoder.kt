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

    /**
     * True for a structurally plausible zlib header (CM=8, CINFO<=7,
     * FCHECK valid). This is only a pre-filter; [decode] still validates the
     * complete stream and the caller must validate the decoded file.
     */
    fun isZlibCandidate(bytes: ByteArray): Boolean {
        if (bytes.size < 2) return false
        val cmf = bytes[0].toInt() and 0xff
        val flg = bytes[1].toInt() and 0xff
        return cmf and 0x80 == 0 && cmf and 0x0f == 8 && (cmf * 256 + flg) % 31 == 0
    }

    /**
     * Inflate a complete, already reassembled zlib stream when the uncompressed
     * size is not known in advance (as on the MLR file-transfer path). Bound the
     * output and require a fully consumed, non-empty stream. Callers must still
     * verify file format/CRC before storing or importing the result.
     */
    fun decode(compressed: ByteArray): ByteArray? {
        if (!isZlibCandidate(compressed) || compressed.size > MAX_BYTES) return null
        val inflater = Inflater()
        return try {
            inflater.setInput(compressed)
            val output = ByteArrayOutputStream(8192)
            val buffer = ByteArray(8192)
            while (!inflater.finished()) {
                val count = inflater.inflate(buffer)
                if (count > 0) {
                    if (count > MAX_BYTES - output.size()) return null
                    output.write(buffer, 0, count)
                } else if (inflater.finished()) {
                    break
                } else if (inflater.needsDictionary() || inflater.needsInput() ||
                    output.size() >= MAX_BYTES
                ) {
                    return null
                } else {
                    return null // no progress: truncated or malformed stream
                }
            }
            if (!inflater.finished() || inflater.remaining != 0 || output.size() == 0) null
            else output.toByteArray()
        } catch (_: DataFormatException) {
            null
        } finally {
            inflater.end()
        }
    }

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
