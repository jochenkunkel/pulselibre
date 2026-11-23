package de.pulselibre

/**
 * Per-run, per-selected-item stage ledger. IDs are monotonically increasing
 * local ordinals, never device UIDs or file hashes. Recording an event twice
 * for the same local item is idempotent.
 */
internal class CirqaSyncPipelineLedger {
    enum class Stage {
        SELECTED,
        QUEUED,
        BLE_WRITE_ACCEPTED,
        RESPONSE_RECEIVED,
        TRANSFER_COMPLETED,
        INTEGRITY_VERIFIED,
        ARCHIVED,
        FIT_DECODED,
        SQLITE_COMMITTED,
        SUCCESSFUL,
        FAILED
    }

    private val events = LinkedHashMap<Long, MutableSet<Stage>>()
    private var nextId = 1L

    @Synchronized
    fun select(): Long {
        val id = nextId++
        events[id] = linkedSetOf(Stage.SELECTED)
        return id
    }

    @Synchronized
    fun record(itemId: Long, stage: Stage): Boolean {
        val itemEvents = events[itemId] ?: return false
        if (stage == Stage.SELECTED) return false
        return itemEvents.add(stage)
    }

    @Synchronized
    fun count(stage: Stage): Int = events.values.count { stage in it }

    @Synchronized
    fun summary(): String = Stage.entries.joinToString(",") { "${it.name.lowercase()}=${count(it)}" }

    @Synchronized
    fun reset() {
        events.clear()
        nextId = 1L
    }
}
