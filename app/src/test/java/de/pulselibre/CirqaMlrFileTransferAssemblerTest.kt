package de.pulselibre

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.ByteArrayOutputStream
import java.util.zip.DeflaterOutputStream

class CirqaMlrFileTransferAssemblerTest {
    @Test fun keepsUncompressedTransferByteForByte() {
        val expected = byteArrayOf(1, 2, 3, 4, 5)
        CirqaMlrFileTransferAssembler(expected.size).use { assembler ->
            assertFalse(assembler.accept(byteArrayOf(1, 2)).complete)
            val last = assembler.accept(byteArrayOf(3, 4, 5))
            assertTrue(last.complete)
            assertEqualsMode(CirqaMlrFileTransferAssembler.Mode.RAW, last.mode)
            assertArrayEquals(expected, assembler.assembledBytes())
        }
    }

    @Test fun stripsEachObservedTransportPrefixAndInflatesAcrossChunks() {
        val expected = ByteArray(257) { (it * 31).toByte() }
        val compressed = ByteArrayOutputStream().also { out ->
            DeflaterOutputStream(out).use { it.write(expected) }
        }.toByteArray()
        CirqaMlrFileTransferAssembler(expected.size).use { assembler ->
            var last: CirqaMlrFileTransferAssembler.ChunkResult? = null
            var offset = 0
            while (offset < compressed.size) {
                val end = minOf(offset + 7, compressed.size)
                val prefix = byteArrayOf((offset and 0xff).toByte(), (end and 0xff).toByte())
                val chunk = assembler.accept(prefix + compressed.copyOfRange(offset, end))
                last = chunk
                assertFalse(chunk.rejected)
                offset = end
            }
            val result = last ?: error("missing compressed chunks")
            assertTrue(result.complete)
            assertEqualsMode(CirqaMlrFileTransferAssembler.Mode.ZLIB, result.mode)
            assertArrayEquals(expected, assembler.assembledBytes())
        }
    }

    @Test fun inflatesZlibStreamWithoutPerNotificationTransportPrefix() {
        val expected = ByteArray(4_841) { (it * 31).toByte() }
        val compressed = ByteArrayOutputStream().also { out ->
            DeflaterOutputStream(out).use { it.write(expected) }
        }.toByteArray()
        CirqaMlrFileTransferAssembler(expected.size).use { assembler ->
            var last: CirqaMlrFileTransferAssembler.ChunkResult? = null
            var offset = 0
            while (offset < compressed.size) {
                val end = minOf(offset + 146, compressed.size)
                val result = assembler.accept(compressed.copyOfRange(offset, end))
                last = result
                assertFalse(result.rejected)
                offset = end
            }
            val result = last ?: error("missing compressed chunks")
            assertTrue(result.complete)
            assertEqualsMode(CirqaMlrFileTransferAssembler.Mode.ZLIB, result.mode)
            assertArrayEquals(expected, assembler.assembledBytes())
        }
    }

    @Test fun preservesRawFileThatStartsWithValidZlibHeader() {
        val expected = byteArrayOf(0x78, 0x9c.toByte(), 0xff.toByte(), 1, 2, 3)
        CirqaMlrFileTransferAssembler(expected.size).use { assembler ->
            assertFalse(assembler.accept(expected.copyOfRange(0, 2)).rejected)
            val result = assembler.accept(expected.copyOfRange(2, expected.size))
            assertTrue(result.complete)
            assertEqualsMode(CirqaMlrFileTransferAssembler.Mode.RAW, result.mode)
            assertArrayEquals(expected, assembler.assembledBytes())
        }
    }

    @Test fun detectsZlibHeaderSplitAcrossShortNotifications() {
        val expected = byteArrayOf(7, 6, 5, 4, 3, 2, 1)
        val compressed = ByteArrayOutputStream().also { out ->
            DeflaterOutputStream(out).use { it.write(expected) }
        }.toByteArray()
        val chunks = compressed.map { byteArrayOf(0x11, 0x22, it) }
        CirqaMlrFileTransferAssembler(expected.size).use { assembler ->
            var last = chunks.firstOrNull()?.let(assembler::accept) ?: error("missing fixture")
            for (chunk in chunks.drop(1)) last = assembler.accept(chunk)
            assertTrue(last.complete)
            assertEqualsMode(CirqaMlrFileTransferAssembler.Mode.ZLIB, last.mode)
            assertArrayEquals(expected, assembler.assembledBytes())
        }
    }

    @Test fun acceptsRawTransfersSplitIntoShortNotifications() {
        val expected = byteArrayOf(3, 2, 1)
        CirqaMlrFileTransferAssembler(expected.size).use { assembler ->
            assembler.accept(byteArrayOf(3))
            assembler.accept(byteArrayOf(2))
            val last = assembler.accept(byteArrayOf(1))
            assertTrue(last.complete)
            assertEqualsMode(CirqaMlrFileTransferAssembler.Mode.RAW, last.mode)
            assertArrayEquals(expected, assembler.assembledBytes())
        }
    }

    @Test fun rejectsOverflowAndWrongInflatedSize() {
        CirqaMlrFileTransferAssembler(2).use { assembler ->
            assertTrue(assembler.accept(byteArrayOf(1, 2, 3)).rejected)
        }

        val compressed = ByteArrayOutputStream().also { out ->
            DeflaterOutputStream(out).use { it.write(byteArrayOf(1, 2, 3)) }
        }.toByteArray()
        CirqaMlrFileTransferAssembler(2).use { assembler ->
            val result = assembler.accept(byteArrayOf(0, 0) + compressed)
            assertTrue(result.rejected)
            assertFalse(result.complete)
        }
    }

    @Test fun rejectsTrailingBytesAfterZlibEnd() {
        val compressed = ByteArrayOutputStream().also { out ->
            DeflaterOutputStream(out).use { it.write(byteArrayOf(9, 8, 7)) }
        }.toByteArray() + byteArrayOf(42)
        CirqaMlrFileTransferAssembler(3).use { assembler ->
            val result = assembler.accept(byteArrayOf(0, 0) + compressed)
            assertTrue(result.rejected)
            assertFalse(result.complete)
        }
    }

    private fun assertEqualsMode(
        expected: CirqaMlrFileTransferAssembler.Mode,
        actual: CirqaMlrFileTransferAssembler.Mode
    ) = org.junit.Assert.assertEquals(expected, actual)
}
