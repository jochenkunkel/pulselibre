package de.pulselibre

import org.junit.Assert.*
import org.junit.Test

class GarminBootstrapObserverTest {
    private fun gfdi(type: Int, body: ByteArray): ByteArray {
        val length = body.size + 6
        val prefix = byteArrayOf(
            length.toByte(), (length ushr 8).toByte(),
            type.toByte(), (type ushr 8).toByte()
        ) + body
        val crc = GarminFrameValidator.crc16Arc(prefix)
        return prefix + byteArrayOf(crc.toByte(), (crc ushr 8).toByte())
    }

    @Test fun deviceInfoThenUnsupportedTracksObservedEvidence() {
        val observer = GarminBootstrapObserver()
        observer.observe(gfdi(5024, byteArrayOf(1)))
        assertEquals(GarminBootstrapObserver.State.DEVICE_INFORMATION_SEEN, observer.state)
        observer.observe(gfdi(5000, byteArrayOf(0x39, 0x8a.toByte(), 2)))
        assertEquals(GarminBootstrapObserver.State.FILESYNC_REJECTED, observer.state)
        assertEquals(1, observer.rejectedFileListRequests)
        assertEquals(1, observer.deviceInformationFrames)
    }

    @Test fun badCrcAndUnrelatedStatusCannotChangeState() {
        val observer = GarminBootstrapObserver()
        val broken = gfdi(5024, byteArrayOf(1))
        broken[4] = 2
        observer.observe(broken)
        observer.observe(gfdi(5000, byteArrayOf(0x39, 0x8a.toByte(), 1)))
        assertEquals(GarminBootstrapObserver.State.WAITING_FOR_DEVICE, observer.state)
        assertEquals(0, observer.rejectedFileListRequests)
    }

    @Test fun acknowledgedRequestDoesNotClaimFileList() {
        val observer = GarminBootstrapObserver()
        observer.observe(gfdi(5000, byteArrayOf(0x39, 0x8a.toByte(), 0)))
        assertEquals(1, observer.acknowledgedFileListRequests)
        assertEquals(GarminBootstrapObserver.State.WAITING_FOR_DEVICE, observer.state)
        observer.observeFileListResponse()
        assertEquals(GarminBootstrapObserver.State.FILE_LIST_SEEN, observer.state)
    }
}
