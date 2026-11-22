package de.pulselibre

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/** Where a table row's values come from; the two sources keep their own clock and are never merged. */
enum class HistorySource { BAND, PHONE }

/** Measured quantity of a table row. */
enum class HistoryMetric { PULSE, SPO2 }

/**
 * One row of the "all data" table: all values of one [source] and [metric] on one local [day],
 * summarised without interpolation. [source] decides the time basis: BAND = measurement time of the band,
 * PHONE = reception time on the phone.
 */
data class HistoryTableRow(
    val day: LocalDate, val source: HistorySource, val metric: HistoryMetric,
    val count: Int, val minimum: Int, val median: Double, val mean: Double, val maximum: Int
)

/** Pure aggregation behind the Verlauf "Alle Daten" table: live (phone) and imported (band) data in one list. */
object HistoryTable {
    /** Newest day first; within a day band before phone, then pulse before SpO2. Days without values have no row. */
    fun rows(snapshot: HistorySnapshot, zone: ZoneId): List<HistoryTableRow> {
        val groups = listOf(
            Triple(HistorySource.BAND, HistoryMetric.PULSE, snapshot.fitHeartRate),
            Triple(HistorySource.PHONE, HistoryMetric.PULSE, snapshot.heartRate),
            Triple(HistorySource.PHONE, HistoryMetric.SPO2, snapshot.oxygen)
        )
        val out = ArrayList<HistoryTableRow>()
        for ((source, metric, points) in groups) {
            points.asSequence()
                .filter { it.receivedAt >= snapshot.start && it.receivedAt <= snapshot.end }
                .groupBy { it.receivedAt.atZone(zone).toLocalDate() }
                .forEach { (day, list) -> out += summarise(day, source, metric, list.map { it.value }) }
        }
        return out.sortedWith(compareByDescending<HistoryTableRow> { it.day }
            .thenBy { it.source.ordinal }.thenBy { it.metric.ordinal })
    }

    private fun summarise(day: LocalDate, source: HistorySource, metric: HistoryMetric, values: List<Int>): HistoryTableRow {
        val sorted = values.sorted()
        val first = (sorted.size - 1) / 2
        val median = if (sorted.size % 2 == 1) sorted[first].toDouble()
            else (sorted[first].toDouble() + sorted[first + 1].toDouble()) / 2.0
        return HistoryTableRow(day, source, metric, sorted.size, sorted.first(), median, sorted.average(), sorted.last())
    }
}
