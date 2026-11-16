package de.pulselibre

import org.junit.Assert.*
import org.junit.Test

class GarminFrameValidatorTest {
    @Test fun standardCrcCheckVector() {
        assertEquals(0xBB3D, GarminFrameValidator.crc16Arc("123456789".toByteArray(Charsets.US_ASCII)))
    }

    private fun fixture(typeLow: Int = 0xB3, typeHigh: Int = 0x13): ByteArray {
        val packet = byteArrayOf(6, 0, typeLow.toByte(), typeHigh.toByte(), 0, 0)
        val crc = GarminFrameValidator.crc16Arc(packet, 0, 4)
        packet[4] = crc.toByte()
        packet[5] = (crc ushr 8).toByte()
        return packet
    }

    @Test fun acceptsCrcValidSyntheticHeader() {
        assertEquals(5043, GarminFrameValidator.inspectVerified(fixture())?.messageType)
    }

    @Test fun rejectsCorruptedPacket() {
        val packet = fixture()
        packet[2] = (packet[2].toInt() xor 1).toByte()
        assertNull(GarminFrameValidator.inspectVerified(packet))
    }

    @Test fun rejectsWrongLengthEvenWithValidChecksum() {
        val packet = fixture()
        packet[0] = 7
        val crc = GarminFrameValidator.crc16Arc(packet, 0, 4)
        packet[4] = crc.toByte()
        packet[5] = (crc ushr 8).toByte()
        assertNull(GarminFrameValidator.inspectVerified(packet))
    }

    @Test fun unknownTypeIsNotInterpretedAsHealthData() {
        assertEquals("UNKNOWN", GarminFrameValidator.inspectVerified(fixture(1, 0))?.knownType)
    }
}
