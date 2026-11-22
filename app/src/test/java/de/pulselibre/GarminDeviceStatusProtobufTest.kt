package de.pulselibre

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** #142/#159: pure wire codec for the Garmin device-status battery request/response. */
class GarminDeviceStatusProtobufTest {
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

    private fun lenField(field: Int, bytes: ByteArray): ByteArray =
        varint((field.toLong() shl 3) or 2L) + varint(bytes.size.toLong()) + bytes

    private fun intField(field: Int, value: Long): ByteArray =
        varint(field.toLong() shl 3) + varint(value)

    private fun response(vararg fields: ByteArray): ByteArray = lenField(3, fields.fold(ByteArray(0)) { a, b -> a + b })
    private fun smart(service: ByteArray): ByteArray = lenField(8, service)

    @Test fun requestIsSmartField8WithEmptyBatteryRequest() {
        // Smart{ deviceStatusService(8) { remoteDeviceBatteryStatusRequest(2) {} } }
        assertEquals("42021200", GarminDeviceStatusProtobuf.batteryStatusRequest().toHex())
    }

    @Test fun parsesOkResponseWithoutInventingRuntime() {
        val resp = response(intField(1, 1), intField(2, 37), intField(3, 1)) // OK, 37 %, NORMAL, no hours
        val parsed = GarminDeviceStatusProtobuf.parseBatteryResponse(smart(resp))
        assertTrue(parsed!!.isOk)
        assertEquals(37, parsed.batteryLevel)
        assertEquals(GarminDeviceStatusProtobuf.STATUS_NORMAL, parsed.statusName)
        assertNull("keine Restlaufzeit erfinden", parsed.hoursRemaining)
    }

    @Test fun parsesChargingAndRuntime() {
        val resp = response(intField(1, 1), intField(2, 80), intField(3, 4), intField(4, 6))
        val parsed = GarminDeviceStatusProtobuf.parseBatteryResponse(smart(resp))!!
        assertEquals(80, parsed.batteryLevel)
        assertEquals(GarminDeviceStatusProtobuf.STATUS_CHARGING, parsed.statusName)
        assertEquals(6, parsed.hoursRemaining)
    }

    @Test fun noRemoteDeviceIsNotPresentedAsBatteryValue() {
        val resp = response(intField(1, 2))
        val parsed = GarminDeviceStatusProtobuf.parseBatteryResponse(smart(resp))!!
        assertFalse(parsed.isOk)
        assertEquals("NO_REMOTE_DEVICE", parsed.responseStatusName)
        assertNull(parsed.batteryLevel)
    }

    @Test fun unknownFieldsAndNotificationsAreIgnored() {
        val notification = lenField(1, intField(1, 1)) // changed notification, not a response
        val resp = response(intField(1, 1), intField(2, 55), intField(3, 5))
        val service = notification + intField(99, 7) + resp // unknown varint field must be skipped
        val parsed = GarminDeviceStatusProtobuf.parseBatteryResponse(smart(service))
        assertEquals(55, parsed!!.batteryLevel)
        assertEquals(GarminDeviceStatusProtobuf.STATUS_FULLY_CHARGED, parsed.statusName)
    }

    @Test fun emptyOrForeignContainerYieldsNull() {
        assertNull(GarminDeviceStatusProtobuf.parseBatteryResponse(ByteArray(0)))
        assertFalse(GarminDeviceStatusProtobuf.containsBatteryResponse(lenField(43, ByteArray(0))))
    }

    private fun ByteArray.toHex(): String = joinToString("") { "%02x".format(it.toInt() and 255) }
}
