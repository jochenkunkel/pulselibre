package de.pulselibre

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

/** #142/#159: notification reassembly for the Garmin battery-status response. */
class GarminDeviceStatusResponseDecoderTest {
    private fun varint(value: Long): ByteArray {
        var v = value
        val out = ArrayList<Byte>()
        do {
            val b = (v and 127).toInt()
            v = v ushr 7
            out.add((if (v == 0L) b else b or 128).toByte())
        } while (v != 0L)
        return out.toByteArray()
    }
    private fun lenField(field: Int, b: ByteArray): ByteArray =
        varint((field.toLong() shl 3) or 2L) + varint(b.size.toLong()) + b
    private fun intField(field: Int, value: Long): ByteArray = varint(field.toLong() shl 3) + varint(value)

    private fun smartResponse(level: Int, status: Int, hours: Int? = null): ByteArray {
        var resp = intField(1, 1) + intField(2, level.toLong()) + intField(3, status.toLong())
        if (hours != null) resp += intField(4, hours.toLong())
        return lenField(8, lenField(3, resp))
    }

    private fun notification(smart: ByteArray, handle: Int = 10): ByteArray {
        val frame = GarminCompactTransport.frame(0x39, 10, 1, smart)
        return byteArrayOf(handle.toByte()) + GarminFileListRequest.cobs(frame)
    }

    @Test fun decodesCompactFramedCobsResponseWithHandle() {
        val payload = notification(smartResponse(37, 1))
        val parsed = GarminDeviceStatusResponseDecoder.decode(listOf(payload))
        assertNotNull(parsed)
        assertEquals(37, parsed!!.batteryLevel)
        assertEquals(GarminDeviceStatusProtobuf.STATUS_NORMAL, parsed.statusName)
    }

    @Test fun decodesChunkedResponseAcrossNotifications() {
        val smart = smartResponse(80, 4, 6)
        val cobs = GarminFileListRequest.cobs(GarminCompactTransport.frame(0x39, 10, 1, smart))
        // Real ML data chunks each carry a one-byte handle prefix (see GarminFileListRequest.mlChunks).
        val chunks = cobs.toList().chunked(7).map { byteArrayOf(10.toByte()) + it.toByteArray() }
        val parsed = GarminDeviceStatusResponseDecoder.decode(chunks)
        assertNotNull(parsed)
        assertEquals(80, parsed!!.batteryLevel)
        assertEquals(6, parsed.hoursRemaining)
    }

    @Test fun keepsAdditionalOfferedTextValuesWithoutInventingBattery() {
        val payload = byteArrayOf(10.toByte()) +
            GarminFileListRequest.cobs("TEST BAND".toByteArray(Charsets.UTF_8))
        val values = GarminDeviceStatusResponseDecoder.extraValues(listOf(payload), 1_800_000_000_000L)
        assertNotNull(values.firstOrNull { it.valueText == "TEST BAND" })
        assertEquals(DeviceMetadataSources.GARMIN_GDI_OTHER, values.first().source)
        assertNull(GarminDeviceStatusResponseDecoder.decode(listOf(payload)))
    }

    @Test fun ignoresForeignAndEmptyNotifications() {
        val foreign = notification(lenField(43, ByteArray(0)))
        assertNull(GarminDeviceStatusResponseDecoder.decode(listOf(foreign)))
        assertNull(GarminDeviceStatusResponseDecoder.decode(emptyList()))
    }
}
