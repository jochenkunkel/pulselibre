package de.pulselibre

import java.io.ByteArrayOutputStream
import java.util.zip.DataFormatException
import java.util.zip.Inflater

/**
 * Reassembles one file carried by MLR data notifications.
 *
 * FileAccess data_size is the uncompressed size. Compressed streams may arrive
 * either directly or with two transport bytes prefixed to every ATT value; the
 * latter are stripped only after a valid zlib header identifies that variant.
 * Uncompressed transfers retain a byte-for-byte fallback.
 */
internal class CirqaMlrFileTransferAssembler(
    private val expectedSize: Int,
    private val maxCompressedBytes: Int = CirqaZlibFileDecoder.MAX_BYTES
) : AutoCloseable {
    enum class Mode { UNDECIDED, RAW, ZLIB }

    data class ChunkResult(
        val complete: Boolean,
        val rejected: Boolean,
        val mode: Mode
    )

    private val raw = ByteArrayOutputStream(minOf(expectedSize.coerceAtLeast(0), 8192))
    private val undecidedPayloads = ArrayList<ByteArray>()
    private var inflater: Inflater? = null
    private var compressionPrefixBytes = TRANSPORT_HEADER_BYTES
    private var rawFallbackBytes = 0
    private var rawFallbackAvailable = true
    private var compressedBytes = 0
    var mode: Mode = Mode.UNDECIDED
        private set
    var complete: Boolean = false
        private set
    var rejected: Boolean = false
        private set

    init {
        require(expectedSize in 1..CirqaZlibFileDecoder.MAX_BYTES)
        require(maxCompressedBytes in 1..CirqaZlibFileDecoder.MAX_BYTES)
    }

    /** Accepts service payload after the one-byte MLR routing prefix was removed. */
    fun accept(payload: ByteArray): ChunkResult {
        if (complete || rejected || payload.isEmpty()) return result()

        if (mode == Mode.UNDECIDED) {
            undecidedPayloads.add(payload.clone())
            if (undecidedPayloads.size > MAX_UNDECIDED_CHUNKS) {
                rejected = true
                undecidedPayloads.clear()
                return result()
            }
            val unprefixedHeader = prefixAfter(0)?.takeIf { isZlibHeader(it[0], it[1]) }
            val prefixedHeader = prefixAfter(TRANSPORT_HEADER_BYTES)
                ?.takeIf { isZlibHeader(it[0], it[1]) }
            if (unprefixedHeader != null || prefixedHeader != null) {
                compressionPrefixBytes =
                    if (unprefixedHeader != null) 0 else TRANSPORT_HEADER_BYTES
                mode = Mode.ZLIB
                inflater = Inflater()
                rawFallbackBytes = undecidedPayloads.sumOf { it.size }
                rawFallbackAvailable = rawFallbackBytes <= expectedSize
                for ((index, chunk) in undecidedPayloads.withIndex()) {
                    acceptCompressed(chunk)
                    if (rejected) {
                        fallbackToRaw()
                        break
                    }
                    if (complete && index != undecidedPayloads.lastIndex) {
                        rejected = true
                        complete = false
                        break
                    }
                }
                if (complete || !rawFallbackAvailable) undecidedPayloads.clear()
                if (mode != Mode.ZLIB) undecidedPayloads.clear()
                return result()
            }
            val rawBytes = undecidedPayloads.sumOf { it.size }
            if (rawBytes >= expectedSize) {
                mode = Mode.RAW
                for ((index, chunk) in undecidedPayloads.withIndex()) {
                    acceptRaw(chunk)
                    if (rejected) break
                    if (complete && index != undecidedPayloads.lastIndex) {
                        rejected = true
                        complete = false
                        break
                    }
                }
                undecidedPayloads.clear()
            }
            return result()
        }

        when (mode) {
            Mode.RAW -> acceptRaw(payload)
            Mode.ZLIB -> {
                if (rawFallbackAvailable) {
                    if (payload.size > expectedSize - rawFallbackBytes) {
                        rawFallbackAvailable = false
                        undecidedPayloads.clear()
                    } else {
                        undecidedPayloads.add(payload.clone())
                        rawFallbackBytes += payload.size
                    }
                }
                acceptCompressed(payload)
                if (rejected) fallbackToRaw()
                if (complete) undecidedPayloads.clear()
            }
            Mode.UNDECIDED -> error("mode must be selected")
        }
        return result()
    }

    private fun acceptRaw(payload: ByteArray) {
        if (payload.size > expectedSize - raw.size()) {
            rejected = true
            return
        }
        raw.write(payload)
        if (raw.size() == expectedSize) complete = true
    }

    private fun acceptCompressed(payload: ByteArray) {
        if (payload.size <= compressionPrefixBytes) {
            rejected = true
            return
        }
        val dataSize = payload.size - compressionPrefixBytes
        if (dataSize > maxCompressedBytes - compressedBytes) {
            rejected = true
            return
        }
        compressedBytes += dataSize
        val offset = compressionPrefixBytes
        val z = inflater ?: run {
            rejected = true
            return
        }
        if (!z.needsInput()) {
            rejected = true
            return
        }
        z.setInput(payload, offset, dataSize)
        val buffer = ByteArray(8192)
        try {
            while (true) {
                val count = z.inflate(buffer)
                if (count > 0) {
                    if (count > expectedSize - raw.size()) {
                        rejected = true
                        return
                    }
                    raw.write(buffer, 0, count)
                }
                if (z.finished()) {
                    if (z.remaining != 0 || raw.size() != expectedSize) rejected = true
                    else complete = true
                    return
                }
                if (z.needsDictionary()) {
                    rejected = true
                    return
                }
                if (count == 0 && z.needsInput()) return
                if (count == 0) {
                    rejected = true
                    return
                }
            }
        } catch (_: DataFormatException) {
            rejected = true
        }
    }

    /**
     * A zlib-looking prefix is only a candidate: ordinary file bytes can begin
     * with the same two values. Keep a bounded raw fallback until the compressed
     * stream is verified, so classifying that raw file does not lose its bytes.
     */
    private fun fallbackToRaw() {
        if (!rawFallbackAvailable) {
            inflater?.end()
            inflater = null
            rejected = true
            complete = false
            undecidedPayloads.clear()
            return
        }
        val candidates = undecidedPayloads.toList()
        inflater?.end()
        inflater = null
        raw.reset()
        rejected = false
        complete = false
        mode = Mode.RAW
        undecidedPayloads.clear()
        for ((index, chunk) in candidates.withIndex()) {
            acceptRaw(chunk)
            if (rejected || complete && index != candidates.lastIndex) break
        }
    }

    private fun prefixAfter(offset: Int): List<Byte>? {
        val bytes = ArrayList<Byte>(2)
        for (chunk in undecidedPayloads) {
            if (chunk.size <= offset) continue
            for (index in offset until chunk.size) {
                bytes.add(chunk[index])
                if (bytes.size == 2) return bytes
            }
        }
        return null
    }

    fun assembledBytes(): ByteArray? =
        if (complete && !rejected) raw.toByteArray() else null

    private fun result(): ChunkResult = ChunkResult(
        complete = complete,
        rejected = rejected,
        mode = mode
    )

    override fun close() {
        inflater?.end()
        inflater = null
    }

    private fun isZlibHeader(cmf: Byte, flg: Byte): Boolean {
        val cmfInt = cmf.toInt() and 0xff
        val flgInt = flg.toInt() and 0xff
        val header = (cmfInt shl 8) or flgInt
        return cmfInt and 0x0f == 8 && cmfInt ushr 4 <= 7 && header % 31 == 0
    }

    private companion object {
        const val TRANSPORT_HEADER_BYTES = 2
        const val MAX_UNDECIDED_CHUNKS = 64
    }
}
