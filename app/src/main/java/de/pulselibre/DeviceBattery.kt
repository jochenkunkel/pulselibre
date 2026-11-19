package de.pulselibre

import android.content.Context

/**
 * Band battery as far as it is actually stored (#159): never hard-coded, never guessed from other values.
 * Source: the newest stored FIT device-info record that carries a battery level.
 */
internal data class BatteryReading(val percent: Int, val statusCode: Int?, val observedAtMs: Long?)

internal object DeviceBatteryFormat {
    /** A stored value older than this is shown as "last known state" instead of a live value. */
    const val STALE_AFTER_MS = 6L * 60 * 60 * 1000
    /** FIT battery_status enum: 6 = charging. Anything else makes no statement about charging. */
    private const val FIT_CHARGING = 6

    data class View(val valueText: String, val freshness: String, val stale: Boolean, val charging: Boolean, val description: String)

    fun describe(reading: BatteryReading, nowMs: Long): View {
        val percent = reading.percent.coerceIn(0, 100)
        val age = reading.observedAtMs?.let { nowMs - it }?.takeIf { it >= 0 }
        val ageText = age?.let { ageText(it) }
        val stale = age == null || age > STALE_AFTER_MS
        val freshness = when {
            age == null -> "Zeitpunkt unbekannt · letzter bekannter Stand"
            stale -> "Veraltet · letzter bekannter Stand $ageText"
            else -> "Stand: $ageText"
        }
        val charging = reading.statusCode == FIT_CHARGING
        val value = "$percent %" + if (charging) " · lädt" else ""
        return View(value, freshness, stale, charging, "Akku: $percent Prozent${if (charging) ", lädt" else ""}. $freshness")
    }

    fun ageText(ms: Long): String {
        val s = ms / 1000
        return when {
            s < 60 -> "vor $s s"
            s < 3600 -> "vor ${s / 60} min"
            s < 172800 -> "vor ${s / 3600} h"
            else -> "vor ${s / 86400} Tagen"
        }
    }

    /** Fill colour by level: red below 15 %, amber below 40 %, green otherwise (soft tones, never the only signal). */
    fun levelColor(percent: Int, dark: Boolean): Int = when {
        percent < 15 -> if (dark) 0xFFE57373.toInt() else 0xFFC62828.toInt()
        percent < 40 -> if (dark) 0xFFFFB74D.toInt() else 0xFFEF8A00.toInt()
        else -> if (dark) 0xFF66D19E.toInt() else 0xFF1F9D63.toInt()
    }
}

internal object DeviceBatteryReader {
    /** Read-only; call on a worker thread. Returns null when no stored record carries a battery level. */
    fun read(context: Context): BatteryReading? = try {
        CirqaDatabase(context.applicationContext).use { database ->
            database.readableDatabase.rawQuery(
                """SELECT battery_level_percent, battery_status_code, observed_at_ms
                   FROM fit_device_metadata
                   WHERE battery_level_percent IS NOT NULL
                   ORDER BY observed_at_ms DESC LIMIT 1""", null
            ).use { c ->
                if (!c.moveToFirst()) null
                else BatteryReading(c.getInt(0), if (c.isNull(1)) null else c.getInt(1), if (c.isNull(2)) null else c.getLong(2))
            }
        }
    } catch (_: Exception) { null }
}

/** Horizontal level bar; the fill width is exactly the percentage. */
internal class DeviceBatteryBar(context: Context, private val percent: Int, private val fill: Int, private val track: Int) :
    android.view.View(context) {
    private val paint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG)
    private val rect = android.graphics.RectF()
    init { importantForAccessibility = IMPORTANT_FOR_ACCESSIBILITY_NO; tag = "battery_bar" }
    override fun onDraw(canvas: android.graphics.Canvas) {
        val h = height.toFloat(); val r = h / 2f
        paint.shader = null; paint.color = track
        rect.set(0f, 0f, width.toFloat(), h); canvas.drawRoundRect(rect, r, r, paint)
        val w = width * percent.coerceIn(0, 100) / 100f
        if (w <= 0f) return
        paint.shader = android.graphics.LinearGradient(0f, 0f, w, 0f, intArrayOf(androidx.core.graphics.ColorUtils.blendARGB(fill, android.graphics.Color.WHITE, 0.22f), fill),
            null, android.graphics.Shader.TileMode.CLAMP)
        rect.set(0f, 0f, w.coerceAtLeast(h), h); canvas.drawRoundRect(rect, r, r, paint)
    }
}
