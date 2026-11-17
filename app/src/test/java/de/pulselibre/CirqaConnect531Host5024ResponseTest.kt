package de.pulselibre

import org.junit.Assert.*
import org.junit.Test

class CirqaConnect531Host5024ResponseTest {
    private fun host(flag: Boolean = false, protocol: Int = 150) =
        CirqaConnect531Host5024Response.Host(flag, protocol, 0x1234, "A", "B", "C")

    @Test fun exactExtensionLayoutAndCrc() {
        val extension = CirqaConnect531Host5024Response.extension(host())
        assertArrayEquals(byteArrayOf(
            113, 0, -1, -1, -1, -1, -1, -1, 0x34, 0x12,
            -1, -1, 1, 65, 1, 66, 1, 67, 1
        ), extension)
        val frame = CirqaConnect531Host5024Response.frame(host())
        assertNotNull(GarminFrameValidator.inspectVerified(frame))
        assertEquals(5000, GarminGfdiHeader.inspect(frame)?.type)
        assertEquals(5024, GarminGfdiHeader.inspect(frame)?.responseTo)
        assertEquals(0, GarminGfdiHeader.inspect(frame)?.responseStatus)
        assertArrayEquals(extension, frame.copyOfRange(7, frame.size - 2))
    }

    @Test fun protocol150RequiresFlagAndThreshold() {
        assertEquals(150, CirqaConnect531Host5024Response.extension(host(true, 150))[0].toInt() and 255)
        assertEquals(113, CirqaConnect531Host5024Response.extension(host(true, 149))[0].toInt() and 255)
        assertEquals(113, CirqaConnect531Host5024Response.extension(host(false, 200))[0].toInt() and 255)
    }

    @Test fun lengthsUseUtf8BytesNotCharacters() {
        val extension = CirqaConnect531Host5024Response.extension(
            host().copy(friendlyName = "ä"))
        assertEquals(2, extension[12].toInt() and 255)
        assertEquals(0xC3, extension[13].toInt() and 255)
        assertEquals(0xA4, extension[14].toInt() and 255)
    }

    @Test(expected = IllegalArgumentException::class)
    fun rejectsOversizedHostString() {
        CirqaConnect531Host5024Response.extension(host().copy(model = "x".repeat(256)))
    }

    @Test(expected = IllegalArgumentException::class)
    fun rejectsOutOfRangeAppVersion() {
        CirqaConnect531Host5024Response.extension(host().copy(appVersion = 65536))
    }
}
