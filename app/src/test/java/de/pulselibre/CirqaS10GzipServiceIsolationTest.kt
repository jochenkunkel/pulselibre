package de.pulselibre

import java.io.ByteArrayOutputStream
import java.util.zip.GZIPInputStream
import java.util.zip.GZIPOutputStream
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * Synthetic S10 wire-shape regression, not a captured payload or a FIT transfer.
 * Observed gzip bytes belong to Smart.2 -> 6 -> 3, while FileAccess is Smart.43.
 * No semantic name for service 2 is inferred from these field numbers.
 */
class CirqaS10GzipServiceIsolationTest {
    @Test fun gzipJsonInOtherServiceIsNeitherFileAccessPageNorPullResult() {
        val json = """{"description":"synthetic","wind":{"speed":0}}""".toByteArray()
        val output = ByteArrayOutputStream()
        GZIPOutputStream(output).use { it.write(json) }
        val gzip = output.toByteArray()
        assertArrayEquals(json, GZIPInputStream(gzip.inputStream()).use { it.readBytes() })

        val smart = bytesField(2, bytesField(6, bytesField(3, gzip)))
        assertNotNull(CirqaFileAccessItemListParser.fields(smart))
        assertNull(CirqaFileAccessItemListParser.parsePage(smart))
        assertNull(CirqaFileAccessItemListParser.parsePullResult(smart))
    }

    @Test fun pullShapedBytesInOtherServiceAreNotAFileAccessSuccess() {
        val pullShaped = bytesField(2, varintField(1, 0) + varintField(3, 42))
        assertNull(CirqaFileAccessItemListParser.parsePullResult(bytesField(2, pullShaped)))
        assertEquals(
            42L,
            CirqaFileAccessItemListParser.parsePullResult(bytesField(43, pullShaped))!!.transferHandle
        )
    }

    @Test fun siblingOtherServiceDoesNotShadowFileAccessInEitherOrder() {
        val otherService = bytesField(2, bytesField(6, bytesField(3, byteArrayOf(1, 2, 3))))
        val pull = bytesField(43, bytesField(2, varintField(1, 0) + varintField(3, 42)))
        val page = bytesField(43, bytesField(10, varintField(1, 0)))
        for (smart in listOf(otherService + pull, pull + otherService)) {
            val result = CirqaFileAccessItemListParser.parsePullResult(smart)!!
            assertEquals(0L, result.resultCode)
            assertEquals(42L, result.transferHandle)
            assertNull(CirqaFileAccessItemListParser.parsePage(smart))
        }
        for (smart in listOf(otherService + page, page + otherService)) {
            val result = CirqaFileAccessItemListParser.parsePage(smart)!!
            assertEquals(0L, result.status)
            assertEquals(0, result.items.size)
            assertNull(CirqaFileAccessItemListParser.parsePullResult(smart))
        }
    }

    private fun varintField(number: Int, value: Long): ByteArray =
        varint((number shl 3).toLong()) + varint(value)

    private fun bytesField(number: Int, value: ByteArray): ByteArray =
        varint(((number shl 3) or 2).toLong()) + varint(value.size.toLong()) + value

    private fun varint(value: Long): ByteArray {
        var remaining = value
        val output = ByteArrayOutputStream()
        do {
            val byte = (remaining and 127).toInt()
            remaining = remaining ushr 7
            output.write(if (remaining == 0L) byte else byte or 128)
        } while (remaining != 0L)
        return output.toByteArray()
    }
}
