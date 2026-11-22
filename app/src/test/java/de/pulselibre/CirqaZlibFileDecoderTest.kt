package de.pulselibre

import java.io.ByteArrayOutputStream
import java.util.zip.Deflater
import java.util.zip.DeflaterOutputStream
import java.util.zip.GZIPOutputStream
import org.junit.Assert.*
import org.junit.Test

class CirqaZlibFileDecoderTest {
    // Independent synthetic empty FIT with header and file CRC, not device data.
    private val fit = byteArrayOf(
        0x0e, 0x20, 0x64, 0, 0, 0, 0, 0,
        0x2e, 0x46, 0x49, 0x54, 0xde.toByte(), 0xee.toByte(), 0, 0
    )

    private fun zlib(bytes: ByteArray): ByteArray = ByteArrayOutputStream().also { output ->
        DeflaterOutputStream(output).use { it.write(bytes) }
    }.toByteArray()

    @Test fun assembledCompressedFitPassesSizeAndIndependentFitCrcChecks() {
        val compressed = zlib(fit)
        // Synthetic notification-like chunks: strip route and transport header
        // independently before decoding; the decoder never guesses wire framing.
        val packets = compressed.toList().chunked(7).map {
            byteArrayOf(0xa0.toByte(), 0) + it.toByteArray()
        }
        val assembled = packets.fold(byteArrayOf()) { bytes, packet ->
            bytes + packet.copyOfRange(2, packet.size)
        }
        val decoded = CirqaZlibFileDecoder.decode(assembled, fit.size)!!
        assertArrayEquals(fit, decoded)
        assertNotNull(FitFileHeader.inspectVerified(decoded))
        assertNull(CirqaZlibFileDecoder.decode(packets[0], fit.size))
    }

    @Test fun rejectsTruncatedCorruptTrailingAndConcatenatedStreams() {
        val compressed = zlib(fit)
        assertNull(CirqaZlibFileDecoder.decode(compressed.copyOf(compressed.size - 1), fit.size))
        val corrupt = compressed.copyOf().also { it[it.lastIndex] = (it.last().toInt() xor 1).toByte() }
        assertNull(CirqaZlibFileDecoder.decode(corrupt, fit.size))
        assertNull(CirqaZlibFileDecoder.decode(compressed + byteArrayOf(0), fit.size))
        assertNull(CirqaZlibFileDecoder.decode(compressed + compressed, fit.size * 2))
    }

    @Test fun rejectsSizeMismatchAndExpansionBeyondExpectedSize() {
        val compressed = zlib(ByteArray(100_000))
        assertNull(CirqaZlibFileDecoder.decode(compressed, 10))
        assertNull(CirqaZlibFileDecoder.decode(zlib(fit), fit.size + 1))
        assertNull(CirqaZlibFileDecoder.decode(zlib(fit), 0))
        assertNull(CirqaZlibFileDecoder.decode(zlib(fit), CirqaZlibFileDecoder.MAX_BYTES + 1))
        assertNull(CirqaZlibFileDecoder.decode(byteArrayOf(), fit.size))
    }

    @Test fun rejectsGzipRawDeflateAndDictionaryStreams() {
        val gzip = ByteArrayOutputStream().also { output ->
            GZIPOutputStream(output).use { it.write(fit) }
        }.toByteArray()
        assertNull(CirqaZlibFileDecoder.decode(gzip, fit.size))
        for (raw in listOf(false, true)) {
            val deflater = Deflater(6, raw)
            val output = ByteArrayOutputStream()
            try {
                deflater.setDictionary(byteArrayOf(1, 2, 3, 4))
                DeflaterOutputStream(output, deflater).use { it.write(fit) }
            } finally {
                deflater.end()
            }
            assertNull(CirqaZlibFileDecoder.decode(output.toByteArray(), fit.size))
        }
    }

    @Test fun outputIsNotAutomaticallyClassifiedAsFitOrImported() {
        val bytes = """{"description":"synthetic"}""".toByteArray()
        val decoded = CirqaZlibFileDecoder.decode(zlib(bytes), bytes.size)!!
        assertArrayEquals(bytes, decoded)
        assertNull(FitFileHeader.inspectVerified(decoded))
    }
}
