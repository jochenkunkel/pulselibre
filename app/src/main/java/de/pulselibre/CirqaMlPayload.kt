package de.pulselibre

/**
 * Payload of an ML/MLR notification after the single routing byte.
 *
 * The first byte carries the handle (bit 7 = flagged MLR routing, handle in bits 6:4; see
 * `GarminMlOffline.inspectDataHandle`). Everything after it is payload. An empty packet yields
 * an empty payload instead of throwing, so a new branch in the receive path cannot crash on a
 * truncated notification (audit A6-42).
 */
internal fun payloadAfterHandle(bytes: ByteArray): ByteArray =
    if (bytes.size <= 1) ByteArray(0) else bytes.copyOfRange(1, bytes.size)
