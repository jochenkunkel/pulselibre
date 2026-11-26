package de.pulselibre

/**
 * R1 (FIT recommendation, 25.11.2026): a measurement that two different FIT files both contain – same device, metric,
 * valid measurement time, unit and exactly the same value – is counted once in charts and daily aggregates. The copy
 * of the file with the smallest SHA-256 is kept; every original row stays stored and exported. Values without FIT
 * provenance or with an unknown time base are never merged, nor are repeats inside one file. This is display
 * aggregation, not proof that both files hold the same physical sample. Same rule as the SQL in
 * [UnifiedMeasurements] (`display_duplicate`).
 */
internal object CrossFileDuplicates {
    private data class Key(val device: String, val metric: String, val timeMs: Long, val unit: String, val valueBits: Long)

    fun countOnce(observations: List<HistoricalObservation>): List<HistoricalObservation> {
        fun eligible(o: HistoricalObservation) = o.provenance != null && o.provenance.timestampOrigin != "UNKNOWN"
        fun key(o: HistoricalObservation) =
            Key(o.deviceId, o.metric, o.measuredAt.toEpochMilli(), o.unit, (o.value + 0.0).toBits())
        val keeper = HashMap<Key, String>()
        for (o in observations) if (eligible(o)) {
            val source = o.provenance!!.sourceFileSha256
            keeper.merge(key(o), source) { a, b -> minOf(a, b) }
        }
        return observations.filter { o -> !eligible(o) || keeper[key(o)] == o.provenance!!.sourceFileSha256 }
    }
}
