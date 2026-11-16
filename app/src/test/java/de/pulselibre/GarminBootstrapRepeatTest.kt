package de.pulselibre

import org.junit.Assert.*
import org.junit.Test

class GarminBootstrapRepeatTest {
    private fun frame(type: Int): ByteArray {
        val header = byteArrayOf(6, 0, type.toByte(), (type ushr 8).toByte())
        val crc = GarminFrameValidator.crc16Arc(header)
        return header + byteArrayOf(crc.toByte(), (crc ushr 8).toByte())
    }

    @Test fun countsRepeatedDeviceInformationWithoutPayloadLogging() {
        val observer = GarminBootstrapObserver()
        repeat(4) { observer.observe(frame(5024)) }
        assertEquals(4, observer.deviceInformationFrames)
        assertEquals(3, observer.repeatedDeviceInformationFrames)
        assertEquals(GarminBootstrapObserver.State.DEVICE_INFORMATION_SEEN, observer.state)
        assertTrue(observer.summary().contains("wiederholte 5024: 3"))
    }

    @Test fun ignoresInvalidAndOtherFrames() {
        val observer = GarminBootstrapObserver()
        observer.observe(frame(5023))
        val invalid = frame(5024)
        invalid[0] = 7
        observer.observe(invalid)
        assertEquals(0, observer.deviceInformationFrames)
        assertEquals(0, observer.repeatedDeviceInformationFrames)
    }
}
