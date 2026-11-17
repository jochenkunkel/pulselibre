package de.pulselibre

/**
 * Classifies passive evidence from a single bounded 5050 diagnostic.
 * A 5050 ACK or 5037 frame alone does not establish historical file access.
 * This policy does not transmit commands or mutate sensor state.
 */
object Cirqa5050Outcome {
    enum class Result {
        NO_RESPONSE_QUEUED,
        WRITE_INCOMPLETE,
        NO_FRESH_5050_ACK,
        ACK_WITHOUT_5037,
        VALID_5037_SIGNAL
    }

    fun classify(
        responseQueued: Boolean,
        allWritesConfirmed: Boolean,
        fresh5050AckConfirmed: Boolean,
        valid5037AfterAck: Boolean
    ): Result = when {
        !responseQueued -> Result.NO_RESPONSE_QUEUED
        !allWritesConfirmed -> Result.WRITE_INCOMPLETE
        !fresh5050AckConfirmed -> Result.NO_FRESH_5050_ACK
        valid5037AfterAck -> Result.VALID_5037_SIGNAL
        else -> Result.ACK_WITHOUT_5037
    }
}
