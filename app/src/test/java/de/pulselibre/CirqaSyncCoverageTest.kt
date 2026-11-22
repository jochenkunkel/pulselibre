package de.pulselibre

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * JVM-only regression for the coverage marker. All inputs are synthetic
 * booleans/strings; no device, BLE transport or health data is involved.
 */
class CirqaSyncCoverageTest {
    @Test fun drainedRunNeverClaimsCompleteDeviceHistory() {
        assertEquals(
            "coverage=device-history-unverified",
            CirqaSyncCoverage.marker(runDrained = true, incompleteReason = null)
        )
    }

    @Test fun markerIsIndependentOfRunStateAndIncompleteReason() {
        val reasons = listOf(null, "none", "transfer-not-drained", "file-processing-failed")
        for (drained in listOf(false, true)) {
            for (reason in reasons) {
                assertEquals(
                    "coverage=device-history-unverified",
                    CirqaSyncCoverage.marker(runDrained = drained, incompleteReason = reason)
                )
            }
        }
    }
}
