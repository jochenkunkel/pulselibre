package de.pulselibre

/**
 * Stall detection for the continuous recorder (#25): a GATT connection can stay open while no measurement arrives
 * (S10, 22.11.: "connected", last heart rate almost three hours old). The recorder then recycles ITS OWN connection
 * through the existing bounded reconnect path; no new BLE command and no device access is involved.
 */
internal object RecorderStall {
    /** No reading for this long after the attempt started (or after the last reading) counts as a stall. */
    const val STALL_AFTER_MS = 10 * 60_000L

    /**
     * @param attemptStartedMs when the current connection attempt began
     * @param lastReadingMs time of the newest reading of THIS attempt, or 0 when none arrived yet
     */
    fun isStalled(attemptStartedMs: Long, lastReadingMs: Long, nowMs: Long, stallAfterMs: Long = STALL_AFTER_MS): Boolean {
        if (attemptStartedMs <= 0 || nowMs < attemptStartedMs) return false
        val reference = if (lastReadingMs >= attemptStartedMs) lastReadingMs else attemptStartedMs
        return nowMs - reference >= stallAfterMs
    }
}
