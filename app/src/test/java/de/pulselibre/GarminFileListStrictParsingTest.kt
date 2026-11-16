package de.pulselibre

import org.junit.Assert.*
import org.junit.Test

class GarminFileListStrictParsingTest {
    private fun probeWithSmart(smart: ByteArray): GarminFileListProbe {
        val probe = GarminFileListProbe()
        val frame = GarminCompactTransport.frame(0x3a, 10, 1, smart)
        probe.observeMlPayload(GarminFileListRequest.cobs(frame))
        return probe
    }

    @Test fun acceptsCompleteEmptyFileListResponse() {
        // Smart field 43 -> FileSync field 10 -> empty FileListResponse.
        val probe = probeWithSmart(byteArrayOf(0xda.toByte(), 0x02, 0x02, 0x52, 0x00))
        assertEquals(1, probe.validLists)
        assertEquals(0, probe.fileEntries)
    }

    @Test fun rejectsMalformedTailInsteadOfCountingFileList() {
        val probe = probeWithSmart(byteArrayOf(0xda.toByte(), 0x02, 0x02, 0x52, 0x00, 0x80.toByte()))
        assertEquals(0, probe.validLists)
    }

    @Test fun rejectsMalformedNestedFileList() {
        val probe = probeWithSmart(byteArrayOf(0xda.toByte(), 0x02, 0x03, 0x52, 0x01, 0x80.toByte()))
        assertEquals(0, probe.validLists)
    }
}
