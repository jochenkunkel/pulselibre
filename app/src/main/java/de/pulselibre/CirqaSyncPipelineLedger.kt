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
        ARCHIVE_CONTENT_PREEXISTED,
        ARCHIVE_CONTENT_NEW,
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

    /** Late GATT failure callback cannot overturn an already received app response. */
    @Synchronized
    fun failWriteIfNoResponse(itemId: Long): Boolean {
        val itemEvents = events[itemId] ?: return false
        if (Stage.RESPONSE_RECEIVED in itemEvents) return false
        return itemEvents.add(Stage.FAILED)
    }

    @Synchronized
    fun summary(): String = Stage.entries.joinToString(",") { "${it.name.lowercase()}=${count(it)}" }

    /**
     * Ordered, privacy-safe per-selected-item progression. The ordinal is local
     * to this run; each hex digit is only the bitset of reached pipeline stages.
     */
    @Synchronized
    fun orderedSummary(): String = events.entries.joinToString(";") { (ordinal, reached) ->
        val mask = Stage.entries.foldIndexed(0) { bit, value, stage ->
            if (stage in reached) value or (1 shl bit) else value
        }
        "$ordinal:${mask.toString(16)}"
    }

    @Synchronized
    fun reset() {
        events.clear()
        nextId = 1L
    }
}
