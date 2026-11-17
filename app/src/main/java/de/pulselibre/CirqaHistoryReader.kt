package de.pulselibre

import android.content.Context
import org.json.JSONObject
import java.time.Instant

/** Read-only, time-bounded projection of stored CIRQA heart-rate and SpO₂ readings. */
class CirqaHistoryReader(private val context: Context) {
    fun read(range: HistoryRange, now: Instant = Instant.now()): HistorySnapshot {
        val start = range.startAt(now)
        val pulse = mutableListOf<HistoryPoint>()
        val oxygen = mutableListOf<HistoryPoint>()
        val lower = start.minusSeconds(1).toString()
        val upper = now.plusSeconds(1).toString()
        val sql = """SELECT payload_json FROM events
            WHERE kind = ? AND service IN (?, ?)
            AND julianday(received_at_utc) >= julianday(?)
            AND julianday(received_at_utc) <= julianday(?)
            ORDER BY received_at_utc, id""".trimIndent()
        CirqaDatabase(context).use { db ->
            db.readableDatabase.rawQuery(sql, arrayOf("reading", "6", "19", lower, upper)).use { cursor ->
                while (cursor.moveToNext()) {
                    val item = try { JSONObject(cursor.getString(0)) } catch (_: Exception) { continue }
                    if (item.optString("type") != "reading" || item.optInt("schema") != 1) continue
                    val time = try { Instant.parse(item.getString("received_at_utc")) } catch (_: Exception) { continue }
                    if (time < start || time > now) continue
                    when (item.optInt("service", -1)) {
                        6 -> item.optLong("bpm", Long.MIN_VALUE).takeIf { it in 1..240 }
                            ?.let { pulse += HistoryPoint(time, it.toInt()) }
                        19 -> item.optLong("spo2_percent", Long.MIN_VALUE).takeIf { it in 1..100 }
                            ?.let { oxygen += HistoryPoint(time, it.toInt()) }
                    }
                }
            }
        }
        return HistorySnapshot(range, start, now, pulse, oxygen)
    }
}
