package de.pulselibre

/**
 * Bounded, metadata-only history of Android's observed GATT state.
 * This is NOT a PulseLibre GATT connection or proof of Garmin Connect ownership.
 */
class CirqaConnectionHistory(private val capacity: Int = 32) {
    init { require(capacity > 0) }
    data class Entry(val timestampMillis: Long, val state: State)
    enum class State { CONNECTED, CONNECTING, DISCONNECTING, DISCONNECTED, UNKNOWN, PERMISSION_DENIED, NOT_PAIRED }
    private val entries = ArrayDeque<Entry>()

    fun observe(timestampMillis: Long, state: State): Boolean {
        require(timestampMillis >= 0)
        if (entries.lastOrNull()?.state == state) return false
        if (entries.size == capacity) entries.removeFirst()
        entries.addLast(Entry(timestampMillis, state))
        return true
    }

    fun snapshot(): List<Entry> = entries.toList()
}
