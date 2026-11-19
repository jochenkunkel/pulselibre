package de.pulselibre

import java.time.LocalDate

/**
 * Data coverage per series (#41): "x von y Tagen", resting pulse and steps reported SEPARATELY – one shared percentage
 * would mislead. Counts complete days before today only (same window rule as [TrendAnalysis]).
 */
internal object DataCoverage {
    data class Line(val name: String, val validDays: Int, val windowDays: Int, val needed: Int) {
        val sufficient get() = validDays >= needed
        val fraction get() = if (windowDays == 0) 0f else validDays.toFloat() / windowDays
        fun text() = "$name: $validDays von $windowDays Tagen"
        fun hint(): String? = if (sufficient) null else "für eine Trendaussage fehlen Tage (mindestens $needed nötig)"
        fun description() = text() + (hint()?.let { ", $it" } ?: "")
    }

    fun line(name: String, daily: Map<LocalDate, Double>, today: LocalDate, windowDays: Int): Line {
        val valid = (1..windowDays).count { daily[today.minusDays(it.toLong())]?.isFinite() == true }
        return Line(name, valid, windowDays, TrendAnalysis.minValidDays(windowDays))
    }
}
