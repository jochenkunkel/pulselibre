package de.pulselibre

import org.junit.Assert.*
import org.junit.Test

class GarminPostBootstrapObserverTest {
    private fun frame(type: Int, body: ByteArray): ByteArray {
        val length = 6 + body.size
        val prefix = byteArrayOf(length.toByte(), (length ushr 8).toByte(),
            type.toByte(), (type ushr 8).toByte()) + body
        val crc = GarminFrameValidator.crc16Arc(prefix)
        return prefix + byteArrayOf(crc.toByte(), (crc ushr 8).toByte())
    }

    @Test fun tracksOnlyVerifiedPostAckMetadata() {
        val observer = GarminPostBootstrapObserver()
        observer.observe(frame(5024, byteArrayOf(1)), false)
        observer.observe(frame(5050, byteArrayOf(1, 2, 3)), true)
        observer.observe(frame(5011, byteArrayOf(4)), true)
        observer.observe(frame(5050, byteArrayOf(1, 2, 3)).also { it[it.lastIndex] = 0 }, true)
        assertEquals(2, observer.afterAckFrames)
        val summary = observer.summary()
        assertTrue(summary.contains("5050=1"))
        assertTrue(summary.contains("5011=1"))
        assertTrue(summary.contains("5050={3=1}"))
        assertFalse(summary.contains("Payload=01"))
    }
}
