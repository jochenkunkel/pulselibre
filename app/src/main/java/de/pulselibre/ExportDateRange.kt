package de.pulselibre

import java.time.LocalDate
import java.time.ZoneId

/** Calendar days in the user's timezone; the final selected day is included. */
fun exportWindowForDates(from: LocalDate, through: LocalDate, zone: ZoneId): ExportWindow {
    require(!through.isBefore(from)) { "End date must not precede start date" }
    val start = from.atStartOfDay(zone).toInstant().toEpochMilli()
    val end = through.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli()
    return ExportWindow(start, end)
}
