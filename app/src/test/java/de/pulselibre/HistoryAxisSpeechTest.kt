package de.pulselibre

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test
import java.time.Duration
import java.time.Instant
import java.time.ZoneId

class HistoryAxisSpeechTest {
    private val zone = ZoneId.of("Europe/Berlin")
    private val end = Instant.parse("2026-11-20T14:24:00Z")

    @Test fun fullDayWindowNamesBothDates() {
        val text = HistoryAxisSpeech.span(end.minus(Duration.ofHours(24)), end, HistoryRange.DAY, zone)
        assertEquals("19.11. 15:24 bis 20.11. 15:24", text)
    }

    @Test fun startAndEndNeverSoundIdenticalInTheDayRange() {
        val text = HistoryAxisSpeech.span(end.minus(Duration.ofHours(24)), end, HistoryRange.DAY, zone)
        val (from, to) = text.split(" bis ")
        assertNotEquals(from, to)
    }
}
