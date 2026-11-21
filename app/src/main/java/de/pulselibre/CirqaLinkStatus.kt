package de.pulselibre

import de.pulselibre.CirqaConnectionHistory.State

/**
 * UI status of the CIRQA link, derived from three independent facts:
 * recorder service flag, Android's GATT state, and the last reading the recorder actually received.
 * A running service alone is never "online"; fresh readings count as proof of an active link even
 * if Android's GATT query briefly lags or reports a stale state during a reconnect.
 */
internal object CirqaLinkStatus {
    /** Readings arrive about every few seconds; beyond this age a link is no longer evidenced by data. */
    const val FRESH_READING_MS = 120_000L

    enum class Kind { ONLINE_RECEIVING, ONLINE_WAITING, RECONNECTING, RECORDER_OFF_DISCONNECTED, UNAVAILABLE }

    data class Result(val kind: Kind, val online: Boolean, val label: String)

    fun evaluate(strings: Strings, recorderRunning: Boolean, gatt: State, lastReadingMs: Long?, nowMs: Long): Result {
        if (gatt == State.NOT_PAIRED || gatt == State.PERMISSION_DENIED) {
            // User-facing wording (#209): no "Android GATT" jargon in the status the user sees.
            return Result(Kind.UNAVAILABLE, false,
                if (gatt == State.PERMISSION_DENIED) strings.get(R.string.link_bt_permission) else CirqaStateMapping.label(strings, gatt))
        }
        val age = lastReadingMs?.takeIf { it in 0..nowMs }?.let { nowMs - it }
        val fresh = recorderRunning && age != null && age <= FRESH_READING_MS
        return when {
            fresh -> Result(Kind.ONLINE_RECEIVING, true, strings.get(R.string.link_receiving, ageText(strings, age!!)))
            gatt == State.CONNECTED -> Result(Kind.ONLINE_WAITING, true,
                strings.get(if (recorderRunning) R.string.link_waiting else R.string.link_gatt_recorder_off))
            recorderRunning -> Result(Kind.RECONNECTING, false,
                if (age != null) strings.get(R.string.link_reconnecting_last, ageText(strings, age)) else strings.get(R.string.link_reconnecting))
            else -> Result(Kind.RECORDER_OFF_DISCONNECTED, false, strings.get(R.string.link_none))
        }
    }

    fun ageText(strings: Strings, ms: Long): String {
        val s = ms / 1000
        return when {
            s < 60 -> strings.get(R.string.dur_s, s)
            s < 3600 -> strings.get(R.string.dur_min, s / 60)
            else -> strings.get(R.string.dur_h_min, s / 3600, (s % 3600) / 60)
        }
    }

    fun contentDescription(strings: Strings, result: Result): String =
        strings.get(R.string.link_status_cd,
            strings.get(if (result.online) R.string.link_outline_green else R.string.link_outline_black), result.label)
}
