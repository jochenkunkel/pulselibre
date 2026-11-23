package de.pulselibre

/**
 * Monotonic measured durations only. No budget estimates are produced.
 * `nowNanos` is injectable for deterministic unit tests.
 */
internal class CirqaSyncTiming(private val nowNanos: () -> Long = System::nanoTime) {
    enum class Stage {
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
        val result = measurements.getValue(active.stage)
        result.count++
        result.totalNanos += elapsed
        return elapsed
    }

    /** Close all outstanding intervals at a terminal boundary, including timed-out work. */
    @Synchronized
    fun stopAll() {
        val now = nowNanos()
        val active = running.values.toList()
        running.clear()
        active.forEach { entry ->
            val result = measurements.getValue(entry.stage)
            result.count++
            result.totalNanos += (now - entry.startedAtNanos).coerceAtLeast(0L)
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

    @Synchronized
    fun reset() {
        running.clear()
        measurements.values.forEach {
            it.count = 0
            it.totalNanos = 0L
        }
    }
}
