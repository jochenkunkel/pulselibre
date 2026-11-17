package de.pulselibre

import org.junit.Assert.*
import org.junit.Test

class CirqaBootstrapGapAnalysisTest {
    @Test fun actualSyntheticDeviceObservationRequiresBootstrapResearch() {
        val evidence = CirqaBootstrapGapAnalysis.Evidence(
            deviceInformationReceived = true,
            minimal5024AckGattConfirmed = true,
            configuration5050Ack = true,
            synchronization5037Observed = false,
            fileListResponseObserved = false,
            compactFileListUnsupported = true
        )
        assertEquals(CirqaBootstrapGapAnalysis.NextStep.VERIFY_POST_CONFIGURATION_BOOTSTRAP,
            CirqaBootstrapGapAnalysis.next(evidence))
        assertTrue(CirqaBootstrapGapAnalysis.explain(evidence).contains("UNSUPPORTED"))
    }

    @Test fun gattAckIsNotAssumedWhenMissing() {
        val evidence = CirqaBootstrapGapAnalysis.Evidence(true, false, true, false, false, false)
        assertEquals(CirqaBootstrapGapAnalysis.NextStep.VERIFY_DEVICE_INFORMATION_ACK,
            CirqaBootstrapGapAnalysis.next(evidence))
    }

    @Test fun fileListIsOnlyConfirmedByActualResponse() {
        val evidence = CirqaBootstrapGapAnalysis.Evidence(true, true, true, true, false, true)
        assertEquals(CirqaBootstrapGapAnalysis.NextStep.INVESTIGATE_FILESYNC_VARIANT,
            CirqaBootstrapGapAnalysis.next(evidence))
        assertEquals(CirqaBootstrapGapAnalysis.NextStep.FILE_LIST_CONFIRMED,
            CirqaBootstrapGapAnalysis.next(evidence.copy(fileListResponseObserved = true)))
    }
}
