package de.pulselibre

import org.junit.Assert.*
import org.junit.Test

class CirqaSyntheticDownloadLimitsTest {
    @Test fun rejectsInvalidSizesAndTimeouts() {
        val b = CirqaSyntheticDownloadLimits()
        assertFalse(b.acceptFile(-1, 0))
        assertFalse(b.acceptFile(CirqaSyntheticDownloadLimits.MAX_FILE_BYTES + 1, 0))
        assertFalse(b.acceptFile(1, -1))
        assertFalse(b.acceptFile(1, CirqaSyntheticDownloadLimits.MAX_SESSION_MILLIS + 1))
        assertEquals(0, b.acceptedFiles)
    }

    @Test fun rejectsTotalBudgetOverflowWithoutMutatingCounters() {
        val b = CirqaSyntheticDownloadLimits()
        repeat(8) { assertTrue(b.acceptFile(CirqaSyntheticDownloadLimits.MAX_FILE_BYTES, 0)) }
        assertEquals(CirqaSyntheticDownloadLimits.MAX_TOTAL_BYTES, b.acceptedBytes)
        assertFalse(b.acceptFile(1, 0))
        assertEquals(8, b.acceptedFiles)
    }

    @Test fun enforcesFileCountEvenForEmptyFiles() {
        val b = CirqaSyntheticDownloadLimits()
        repeat(CirqaSyntheticDownloadLimits.MAX_FILES) { assertTrue(b.acceptFile(0, 0)) }
        assertFalse(b.acceptFile(0, 0))
    }

    @Test fun rejectsLongOverflowCandidates() {
        val b = CirqaSyntheticDownloadLimits()
        assertFalse(b.acceptFile(Long.MAX_VALUE, 0))
        assertEquals(0L, b.acceptedBytes)
    }
}
