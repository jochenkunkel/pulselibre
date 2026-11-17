package de.pulselibre

import org.junit.Assert.*
import org.junit.Test

class CirqaSyncExperimentGateTest {
    private fun valid() = CirqaSyncExperimentGate.Observation(
        userConfirmed = true, recorderStopped = true,
        gattWriteConfirmed = true, configurationAckConfirmed = true,
        negativeProtocolResponse = false, unexpectedFrame = false,
        connectionLost = false, repeatedConfigurationRequest = false,
        elapsedMillis = 1000L, crcValid5037Observed = false
    )

    @Test fun validSessionCanObserveButDoesNotClaimSync() {
        assertEquals(CirqaSyncExperimentGate.Decision.CONTINUE_OBSERVING,
            CirqaSyncExperimentGate.evaluate(valid()))
    }

    @Test fun onlyValidated5037CountsAsSignal() {
        assertEquals(CirqaSyncExperimentGate.Decision.SYNC_SIGNAL_OBSERVED,
            CirqaSyncExperimentGate.evaluate(valid().copy(crcValid5037Observed = true)))
    }

    @Test fun allSafetyViolationsFailClosed() {
        val base = valid()
        val invalid = listOf(
            base.copy(userConfirmed = false),
            base.copy(recorderStopped = false),
            base.copy(gattWriteConfirmed = false),
            base.copy(configurationAckConfirmed = false),
            base.copy(negativeProtocolResponse = true),
            base.copy(unexpectedFrame = true),
            base.copy(connectionLost = true),
            base.copy(repeatedConfigurationRequest = true),
            base.copy(elapsedMillis = -1),
            base.copy(elapsedMillis = 10_001),
            base.copy(negativeProtocolResponse = true, crcValid5037Observed = true)
        )
        invalid.forEach { assertEquals(CirqaSyncExperimentGate.Decision.STOP,
            CirqaSyncExperimentGate.evaluate(it)) }
    }

    @Test fun observationDeadlineIsInclusive() {
        assertEquals(CirqaSyncExperimentGate.Decision.CONTINUE_OBSERVING,
            CirqaSyncExperimentGate.evaluate(valid().copy(elapsedMillis = 10_000)))
    }
}
