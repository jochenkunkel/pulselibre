package de.pulselibre

/**
 * Monotonic measured durations only. No budget estimates are produced.
 * `nowNanos` is injectable for deterministic unit tests.
 */
internal class CirqaSyncTiming(private val nowNanos: () -> Long = System::nanoTime) {
    enum class Stage {
        RUN_TOTAL,
        BOOTSTRAP,
        ITEM_LIST_RESPONSE,
        SESSION_PAGE,
        TRANSACTION_FOLLOWUP,
        FILE_TRANSFER,
        VALIDATION,
        STORAGE,
        ARCHIVE,
        FIT_IMPORT,
        SQLITE_COMMIT
    }

    data class Measurement(val count: Int, val totalNanos: Long)

    private data class Running(val stage: Stage, val startedAtNanos: Long)
    private val running = HashMap<String, Running>()
    private val measurements = Stage.entries.associateWith { MutableMeasurement() }
    private val measurementsByKey = LinkedHashMap<String, MutableMeasurement>()

    private class MutableMeasurement(var count: Int = 0, var totalNanos: Long = 0)

    @Synchronized
    fun start(key: String, stage: Stage): Boolean {
        if (running.containsKey(key)) return false
        running[key] = Running(stage, nowNanos())
        return true
    }

    @Synchronized
    fun stop(key: String, expectedStage: Stage? = null): Long? {
        val active = running[key] ?: return null
        if (expectedStage != null && active.stage != expectedStage) return null
        running.remove(key)
        val elapsed = (nowNanos() - active.startedAtNanos).coerceAtLeast(0L)
        record(active.stage, key, elapsed)
        return elapsed
    }

    /** Close all outstanding intervals at a terminal boundary, including timed-out work. */
    @Synchronized
    fun stopAll() {
        val now = nowNanos()
        val active = running.toMap()
        running.clear()
        active.forEach { (key, entry) ->
            record(entry.stage, key, (now - entry.startedAtNanos).coerceAtLeast(0L))
        }
    }

    @Synchronized
    fun snapshot(): Map<Stage, Measurement> =
        measurements.mapValues { Measurement(it.value.count, it.value.totalNanos) }

    @Synchronized
    fun redactedSummary(): String =
        Stage.entries.joinToString(",") { stage ->
            val measurement = measurements.getValue(stage)
            "${stage.name.lowercase()}Count=${measurement.count}," +
                "${stage.name.lowercase()}Ms=${measurement.totalNanos / 1_000_000}"
        }

    /** Only emit per-item measurements for keys containing a local ordinal. */
    @Synchronized
    fun perItemSummary(): String {
        val pattern = Regex("""item:(\d+):([a-z-]+)""")
        return measurementsByKey.entries.mapNotNull { (key, measurement) ->
            val match = pattern.matchEntire(key) ?: return@mapNotNull null
            val ordinal = match.groupValues[1].toLongOrNull() ?: return@mapNotNull null
            Triple(ordinal, match.groupValues[2], measurement.totalNanos / 1_000_000)
        }.sortedWith(compareBy<Triple<Long, String, Long>> { it.first }.thenBy { it.second })
            .groupBy({ it.first }, { "${it.second}:${it.third}ms" })
            .entries.joinToString(";") { (ordinal, values) ->
                "item$ordinal[${values.joinToString(",")}]"
            }
    }

    @Synchronized
    fun reset() {
        running.clear()
        measurements.values.forEach {
            it.count = 0
            it.totalNanos = 0L
        }
        measurementsByKey.clear()
    }

    private fun record(stage: Stage, key: String, elapsedNanos: Long) {
        val aggregate = measurements.getValue(stage)
        aggregate.count++
        aggregate.totalNanos += elapsedNanos
        val perKey = measurementsByKey.getOrPut(key) { MutableMeasurement() }
        perKey.count++
        perKey.totalNanos += elapsedNanos
    }
}
