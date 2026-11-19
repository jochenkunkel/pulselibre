package de.pulselibre

import java.text.NumberFormat
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * Activity atlas (#41, "Fitness Atlas"): calendar grid of the last daily step totals.
 * Pure logic. Colour levels are neutral (relative step amount of the observed days), never a health judgement;
 * a missing day is "no verified data", not zero steps. Today is marked as not finished.
 */
internal object ActivityAtlas {
    enum class Level { NO_DATA, LOW, MID, HIGH }

    /** [date] == null marks a padding cell outside the window (calendar alignment). */
    data class Cell(val date: LocalDate?, val steps: Int?, val level: Level, val partialToday: Boolean)

    data class Model(
        val windowDays: Int, val start: LocalDate, val end: LocalDate,
        val cells: List<Cell>, val weeks: Int, val validDays: Int
    ) {
        val missingDays get() = windowDays - validDays
        fun summary(): String = "$validDays von $windowDays Tagen mit verifizierten Schrittwerten"
    }

    /** Monday-first calendar-aligned grid (7 columns) for the last [windowDays] days ending [today]. */
    fun build(daily: Map<LocalDate, Double>, today: LocalDate, windowDays: Int): Model {
        require(windowDays in 1..366)
        val start = today.minusDays(windowDays - 1L)
        val values = (0 until windowDays).associate { i ->
            val d = start.plusDays(i.toLong())
            d to daily[d]?.takeIf { it.isFinite() && it >= 0 }?.let { Math.round(it).toInt() }
        }
        // Levels use completed days only: today's counter is still running.
        val completed = values.filter { it.key != today }.values.filterNotNull().sorted()
        val low = percentile(completed, 1.0 / 3)
        val high = percentile(completed, 2.0 / 3)
        fun level(v: Int?): Level = when {
            v == null -> Level.NO_DATA
            completed.size < 3 -> Level.MID                // too few days for relative classes: one neutral class
            v <= low -> Level.LOW
            v <= high -> Level.MID
            else -> Level.HIGH
        }
        val padBefore = (start.dayOfWeek.value - DayOfWeek.MONDAY.value)       // Monday = 0
        val padAfter = (7 - ((padBefore + windowDays) % 7)) % 7
        val cells = ArrayList<Cell>(padBefore + windowDays + padAfter)
        repeat(padBefore) { cells += Cell(null, null, Level.NO_DATA, false) }
        for (i in 0 until windowDays) {
            val d = start.plusDays(i.toLong()); val v = values[d]
            cells += Cell(d, v, level(v), d == today && v != null)
        }
        repeat(padAfter) { cells += Cell(null, null, Level.NO_DATA, false) }
        return Model(windowDays, start, today, cells, cells.size / 7, values.values.count { it != null })
    }

    private fun percentile(sorted: List<Int>, q: Double): Int =
        if (sorted.isEmpty()) 0 else sorted[((sorted.size - 1) * q).toInt().coerceIn(0, sorted.size - 1)]

    private val dateFormat = DateTimeFormatter.ofPattern("EEEE, d. MMMM yyyy", Locale.GERMANY)
    private val number = NumberFormat.getIntegerInstance(Locale.GERMANY)

    /** Detail text for a tapped cell. Missing is never "0 Schritte". */
    fun describe(cell: Cell): String {
        val date = cell.date ?: return ""
        val day = date.format(dateFormat)
        return when {
            cell.steps == null -> "$day\nKeine verifizierten Tagesdaten."
            cell.partialToday -> "$day\nBisher ${number.format(cell.steps)} Schritte (heute, noch nicht abgeschlossen)."
            else -> "$day\nLetzter verifizierter Schrittstand: ${number.format(cell.steps)}."
        }
    }

    fun levelLabel(level: Level) = when (level) {
        Level.NO_DATA -> "keine Daten"; Level.LOW -> "weniger erfasste Schritte"; Level.MID -> "mittlere Schrittzahl"; Level.HIGH -> "mehr erfasste Schritte"
    }
}
