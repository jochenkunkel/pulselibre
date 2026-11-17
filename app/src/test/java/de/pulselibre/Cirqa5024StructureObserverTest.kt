package de.pulselibre

import org.junit.Assert.*
import org.junit.Test

class Cirqa5024StructureObserverTest {
    private fun frame(type: Int, body: ByteArray): ByteArray {
        val length = body.size + 6
        val prefix = byteArrayOf(length.toByte(), (length ushr 8).toByte(),
            type.toByte(), (type ushr 8).toByte()) + body
        val crc = GarminFrameValidator.crc16Arc(prefix)
        return prefix + byteArrayOf(crc.toByte(), (crc ushr 8).toByte())
    }

    @Test fun reportsOnlyLayoutMetadata() {
        val observer = Cirqa5024StructureObserver()
        val body = byteArrayOf(1, 0, 2, 0, 4, 3, 2, 1, 5, 0, 6, 0,
            1, 65, 1, 66, 1, 67, 99)
        observer.observe(frame(5024, body))
        val report = observer.summary()
        assertTrue(report.contains("geparst=1"))
        assertTrue(report.contains("optionale Restlängen={1=1}"))
        assertFalse(report.contains("ABC"))
    }

    @Test fun ignoresBadCrcAndOtherTypes() {
        val observer = Cirqa5024StructureObserver()
        val bad = frame(5024, byteArrayOf(1))
        bad[bad.lastIndex] = (bad.last().toInt() xor 1).toByte()
        observer.observe(bad)
        observer.observe(frame(5050, byteArrayOf(0)))
        assertTrue(observer.summary().contains("Frames=0"))
    }

    @Test fun malformed5024DoesNotCrash() {
        val observer = Cirqa5024StructureObserver()
        observer.observe(frame(5024, byteArrayOf(1)))
        assertTrue(observer.summary().contains("nicht-passend=1"))
    }
}
