package de.pulselibre

import org.junit.Assert.*
import org.junit.Test

class GarminFileListPaginationTest {
    @Test fun unfilteredRequestMatchesExistingWireFixture() {
        assertArrayEquals(byteArrayOf(0xda.toByte(), 0x02, 0x02, 0x4a, 0x00),
            GarminFileListPagination.smartRequest())
        assertArrayEquals(GarminFileListRequest.gfdi(), GarminFileListPagination.gfdi(10, 1))
    }

    @Test fun cursorAndPageAreSeparateUnfilteredRequests() {
        assertArrayEquals(byteArrayOf(0xda.toByte(), 0x02, 0x04, 0x4a, 0x02, 0x08, 0x7f),
            GarminFileListPagination.smartRequest(cursorId = 127))
        assertArrayEquals(byteArrayOf(0xda.toByte(), 0x02, 0x05, 0x4a, 0x03, 0x10, 0x80.toByte(), 0x01),
            GarminFileListPagination.smartRequest(startPageId = 128))
    }

    @Test fun disallowsAmbiguousOrNegativePagination() {
        try {
            GarminFileListPagination.smartRequest(cursorId = 1, startPageId = 2)
            fail("cursor and page cannot coexist")
        } catch (_: IllegalArgumentException) {}
        try {
            GarminFileListPagination.smartRequest(cursorId = -1)
            fail("negative cursor cannot be encoded")
        } catch (_: IllegalArgumentException) {}
    }
}
