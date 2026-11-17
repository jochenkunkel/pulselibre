package de.pulselibre

/**
 * Compares *separate* real-device 5050 runs without conflating GATT success,
 * protocol ACK and a demonstrated synchronization transition.
 * Offline-only; never initiates Bluetooth traffic.
 */
object Cirqa5050ExperimentComparison {
    data class Run(
        val syncBitAdvertised: Boolean,
        val writeConfirmed: Boolean,
        val fresh5050Ack: Boolean,
        val valid5037AfterAck: Boolean,
        val fileListResponse: Boolean,
        val compactRxFrames: Int
    ) {
        init { require(compactRxFrames >= 0) }
    }

    enum class Outcome {
        NOT_CONFIRMED, ACK_ONLY, SYNC_SIGNAL, FILE_LIST_OBSERVED
    }

    fun outcome(run: Run): Outcome = when {
        !run.writeConfirmed || !run.fresh5050Ack -> Outcome.NOT_CONFIRMED
        run.fileListResponse -> Outcome.FILE_LIST_OBSERVED
        run.valid5037AfterAck -> Outcome.SYNC_SIGNAL
        else -> Outcome.ACK_ONLY
    }

    enum class Comparison {
        INSUFFICIENT_EVIDENCE, SAME_ACK_ONLY, SYNC_BIT_ADVANCED,
        ZERO_MASK_ADVANCED, BOTH_SYNC_SIGNAL, BOTH_FILE_LIST
    }

    fun compare(zeroMask: Run, syncBit: Run): Comparison {
        require(!zeroMask.syncBitAdvertised && syncBit.syncBitAdvertised)
        val a = outcome(zeroMask)
        val b = outcome(syncBit)
        if (a == Outcome.NOT_CONFIRMED || b == Outcome.NOT_CONFIRMED)
            return Comparison.INSUFFICIENT_EVIDENCE
        if (a == Outcome.ACK_ONLY && b == Outcome.ACK_ONLY)
            return Comparison.SAME_ACK_ONLY
        if (a == Outcome.FILE_LIST_OBSERVED && b == Outcome.FILE_LIST_OBSERVED)
            return Comparison.BOTH_FILE_LIST
        if (a >= Outcome.SYNC_SIGNAL && b >= Outcome.SYNC_SIGNAL)
            return Comparison.BOTH_SYNC_SIGNAL
        return if (b > a) Comparison.SYNC_BIT_ADVANCED else Comparison.ZERO_MASK_ADVANCED
    }
}
