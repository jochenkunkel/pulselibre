package de.pulselibre

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Test

class CirqaRfcommStreamFramerTest {
    private fun b(vararg v: Int) = ByteArray(v.size) { v[it].toByte() }

    @Test fun decodesSingleFrameWithEmbeddedZero() {
        val out = CirqaRfcommStreamFramer().accept(b(3, 0x11, 0x22, 2, 0x33, 0))
        assertEquals(1, out.size)
        assertArrayEquals(b(0x11, 0x22, 0, 0x33), out[0])
    }

    @Test fun reassemblesFrameSplitAcrossChunks() {
        val f = CirqaRfcommStreamFramer()
        assertEquals(0, f.accept(b(3, 0x11)).size)
        assertEquals(0, f.accept(b(0x22, 2)).size)
        val out = f.accept(b(0x33, 0))
        assertArrayEquals(b(0x11, 0x22, 0, 0x33), out.single())
    }

    @Test fun splitsCoalescedFramesAndIgnoresEmptyOnes() {
        val out = CirqaRfcommStreamFramer().accept(b(2, 0x11, 0, 0, 2, 0x22, 0))
        assertEquals(listOf(listOf<Byte>(0x11), listOf<Byte>(0x22)), out.map { it.toList() })
    }

    @Test fun rejectsInvalidFrameAndKeepsGoing() {
        val f = CirqaRfcommStreamFramer()
        val out = f.accept(b(9, 0x11, 0, 2, 0x22, 0)) // code 9 claims 8 more bytes
        assertEquals(1, f.rejected)
        assertEquals(1, f.framesDecoded)
        assertArrayEquals(b(0x22), out.single())
    }

    @Test fun rejectsOversizedFrameWithoutUnboundedGrowth() {
        val f = CirqaRfcommStreamFramer(maxFrameBytes = 256)
        f.accept(ByteArray(1000) { 1 })
        assertEquals(3, f.rejected)
    }

    @Test fun resetDropsPartialFrame() {
        val f = CirqaRfcommStreamFramer()
        f.accept(b(3, 0x11))
        f.reset()
        assertEquals(0, f.accept(b(0)).size)
    }

    @Test(expected = IllegalArgumentException::class) fun rejectsTooSmallLimit() { CirqaRfcommStreamFramer(10) }
}
