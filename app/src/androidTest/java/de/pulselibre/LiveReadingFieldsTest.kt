package de.pulselibre

import androidx.test.ext.junit.runners.AndroidJUnit4
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class LiveReadingFieldsTest {
    private fun item(json: String) = JSONObject(json)

    @Test fun heartRateReadingBecomesBpmAndRestingRows() {
        val rows = LiveReadingFields.rows(7, item(
            """{"schema":1,"type":"reading","service":6,"received_at_utc":"2026-11-19T10:00:00Z","bpm":80,"resting_bpm":60}"""))
        assertEquals(listOf("bpm", "resting_bpm"), rows.map { it.metric })
        assertEquals(listOf(80.0, 60.0), rows.map { it.value })
        assertEquals("event-7-bpm", rows[0].sampleId)
        assertEquals(1_795_082_400_000L, rows[0].timeMs)
    }

    @Test fun missingNullOrNonNumericFieldsAreSkipped() {
        val rows = LiveReadingFields.rows(1, item(
            """{"schema":1,"type":"reading","service":6,"received_at_utc":"2026-11-19T10:00:00Z","bpm":null,"resting_bpm":"x"}"""))
        assertTrue(rows.isEmpty())
    }

    @Test fun foreignSchemaTypeServiceOrBadTimeGiveNoRows() {
        assertTrue(LiveReadingFields.rows(1, item("""{"schema":2,"type":"reading","service":6,"received_at_utc":"2026-11-19T10:00:00Z","bpm":80}""")).isEmpty())
        assertTrue(LiveReadingFields.rows(1, item("""{"schema":1,"type":"status","service":6,"received_at_utc":"2026-11-19T10:00:00Z","bpm":80}""")).isEmpty())
        assertTrue(LiveReadingFields.rows(1, item("""{"schema":1,"type":"reading","service":99,"received_at_utc":"2026-11-19T10:00:00Z","bpm":80}""")).isEmpty())
        assertTrue(LiveReadingFields.rows(1, item("""{"schema":1,"type":"reading","service":6,"received_at_utc":"kaputt","bpm":80}""")).isEmpty())
    }
}
