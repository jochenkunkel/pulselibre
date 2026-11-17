package de.pulselibre

import org.junit.Assert.assertEquals
import org.junit.Test

class Cirqa5050ExperimentComparisonTest {
    private fun run(sync: Boolean, ack: Boolean = true, signal: Boolean = false,
                    list: Boolean = false) = Cirqa5050ExperimentComparison.Run(
        syncBitAdvertised = sync, writeConfirmed = true, fresh5050Ack = ack,
        valid5037AfterAck = signal, fileListResponse = list, compactRxFrames = 0
    )

    @Test fun twoAcknowledgementsDoNotProveSync() {
        assertEquals(Cirqa5050ExperimentComparison.Comparison.SAME_ACK_ONLY,
            Cirqa5050ExperimentComparison.compare(run(false), run(true)))
    }

    @Test fun syncBitImprovementRequires5037Evidence() {
        assertEquals(Cirqa5050ExperimentComparison.Comparison.SYNC_BIT_ADVANCED,
            Cirqa5050ExperimentComparison.compare(run(false), run(true, signal = true)))
    }

    @Test fun unconfirmedWriteOrAckIsInsufficient() {
        assertEquals(Cirqa5050ExperimentComparison.Comparison.INSUFFICIENT_EVIDENCE,
            Cirqa5050ExperimentComparison.compare(run(false), run(true, ack = false)))
    }

    @Test fun fileListIsStrongestEvidence() {
        assertEquals(Cirqa5050ExperimentComparison.Outcome.FILE_LIST_OBSERVED,
            Cirqa5050ExperimentComparison.outcome(run(true, list = true)))
    }
}
