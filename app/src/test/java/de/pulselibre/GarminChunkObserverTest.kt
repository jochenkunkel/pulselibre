package de.pulselibre

import org.junit.Assert.*
import org.junit.Test

class GarminChunkObserverTest {
    @Test fun verifiedChunkMetadataIsCountedWithoutPayload() {
        val observer = GarminChunkObserver()
        val body = ByteArray(14)
        observer.observe(GarminCompactTransport.frame(0x2b, 10, 1, body.copyOfRange(2, body.size)))
        assertEquals(1, observer.observedChunks)
        assertEquals(0, observer.invalidChunks)
        assertEquals(1, observer.kinds[0x2b])
    }

    @Test fun incompleteAndCorruptChunksAreIgnored() {
        val observer = GarminChunkObserver()
        observer.observe(GarminCompactTransport.frame(0x2b, 10, 1, byteArrayOf(1)))
        assertEquals(1, observer.invalidChunks)
        val corrupt = GarminCompactTransport.frame(0x2c, 10, 1, byteArrayOf(1))
        corrupt[4] = (corrupt[4].toInt() xor 1).toByte()
        observer.observe(corrupt)
        assertEquals(0, observer.observedChunks)
    }
}
